package de.komoot.photon.opensearch;

import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@NullMarked
public class OpenSearchImporter implements Importer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String OS_DEFAULT_REFRESH_INTERVAL = "1s";
    private static final int PROGRESS_INTERVAL = 50000;
    private static final int MAX_BULK_RETRIES = 4;
    private static final long INITIAL_RETRY_DELAY_MS = 500L;
    /**
     * Estimated bytes per geometry coordinate in the GeoJSON encoding (two formatted doubles
     * plus brackets and separators). Deliberately generous so the byte estimate runs high and
     * we flush early rather than risk a 413.
     */
    static final int BYTES_PER_GEOMETRY_POINT = 40;

    /** Generous flat allowance for a document's non-geometry fields plus its bulk action line. */
    static final int BASE_DOC_BYTES = 1024;


    private final OpenSearchAsyncClient client;
    @Nullable private String originalRefreshInterval = null;

    private final List<@Nullable CompletableFuture<BulkResponse>> bulks = new ArrayList<>();
    private final BlockingQueue<Integer> doneBulks;

    private BulkRequest.Builder builder = new BulkRequest.Builder();
    private long bytes = 0;
    private static final long maxBulkBytes = 10 * 1024 * 1024;

    private final long startMillis;
    private long counter = 0;
    private boolean hasPrintedNoUpdates = false;

    public OpenSearchImporter(OpenSearchTransport transport, int threads) {
        this.client = new OpenSearchAsyncClient(transport);
        this.doneBulks = new ArrayBlockingQueue<>(threads);


        for (int i = 0; i < threads; ++i) {
            bulks.add(null);
            doneBulks.add(i);
        }

        this.startMillis = System.currentTimeMillis();
    }

    public void disableRefresh() {
        var sclient = new OpenSearchClient(client._transport());
        if (originalRefreshInterval == null) {
            originalRefreshInterval = readRefreshInterval(sclient);
        }

        if (!setRefreshInterval(sclient, "-1")) {
            originalRefreshInterval = null;
        }
    }

    @Override
    public void add(Iterable<PhotonDoc> docs) {
        String placeID = null;
        int objectId = 0;
        for (var doc : docs) {
            if (objectId == 0) {
                placeID = doc.getPlaceId();
            }
            if (placeID == null) {
                if (!hasPrintedNoUpdates) {
                    LOGGER.warn("Documents have no place_id. Updates will not be possible.");
                    hasPrintedNoUpdates = true;
                }
                builder.operations(op -> op.create(i -> i.index(PhotonIndex.NAME).document(doc)));
            } else {
                final String uuid = PhotonDoc.makeUid(placeID, objectId++);
                builder.operations(op -> op.create(i -> i.index(PhotonIndex.NAME).id(uuid).document(doc)));
            }
            bytes += estimateSize(doc);
            // A single doc over MAX_BULK_BYTES still flushes on its own; an oversized request
            // can't be split any smaller anyway.
            if (bytes >= maxBulkBytes) {
                flush();
            }

            if (++counter % PROGRESS_INTERVAL == 0) {
                final double documentsPerSecond = 1000d * counter / (System.currentTimeMillis() - startMillis);
                LOGGER.info("Imported {} documents [{}/second]", counter, documentsPerSecond);
            }
        }
    }

    @Override
    public void flush() {
        final var bulk = builder.build();
        builder = new BulkRequest.Builder();
        bytes = 0;

        try {
            int slot = doneBulks.take();
            finishSlot(slot);
            sendWithRetry(slot, bulk, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted.");
        }
    }

    @Override
    public void finish() {
        if (bytes > 0) {
            flush();
        }

        boolean interrupted = false;
        Throwable error = null;
        int todoSlots = 0;
        for (var b : bulks) {
            if (b != null) {
                ++todoSlots;
            }
        }

        while (todoSlots > 0) {
            int slot;
            try {
                slot = doneBulks.take();
            } catch (InterruptedException e) {
                interrupted = true;
                continue;
            }
            try {
                finishSlot(slot);
            } catch (InterruptedException e) {
                interrupted = true;
            } catch (Exception e) {
                error = e;
            }
            --todoSlots;
        }

        var syncClient = new OpenSearchClient(client._transport());
        if (originalRefreshInterval != null) {
            setRefreshInterval(syncClient, originalRefreshInterval);
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted.");
        }

        if (error != null) {
            throw new RuntimeException("Error processing final bulks", error);
        }

        LOGGER.info("Finished import of {} photon documents. (Total processing time: {}s)",
                counter, (System.currentTimeMillis() - startMillis) / 1000);

        try {
            syncClient.indices().refresh(r -> r.index(PhotonIndex.NAME));
        } catch (IOException e) {
            LOGGER.warn("Refresh of database failed", e);
        }
    }

    private void finishSlot(int slot) throws InterruptedException {
        var prevRequest = bulks.set(slot, null);
        if (prevRequest != null) {
            BulkResponse response;
            try {
                response = prevRequest.get();
            } catch (ExecutionException e) {
                throw new RuntimeException("Error inserting batch.", e);
            }
            if (response.errors()) {
                throw new RuntimeException(BulkRetryHelper.logAndCountItemErrors(response) + " failed items in bulk");
            }
        }
    }

    private boolean sendWithRetry(int slot, BulkRequest bulk, int retry) {
        try {
            var request = client.bulk(bulk).whenComplete((b, t) -> {
                if (retry < MAX_BULK_RETRIES && t != null && isRepeatableBulkOp(t)) {
                    long delayMs = INITIAL_RETRY_DELAY_MS * (1L << (retry - 1));
                    LOGGER.warn("Bulk rejected (429); retrying in {} ms (attempt {}/{})",
                            delayMs, retry + 1, MAX_BULK_RETRIES);

                    CompletableFuture.supplyAsync(() -> sendWithRetry(slot, bulk, retry + 1),
                            CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS));
                } else {
                    doneBulks.add(slot);
                }
            });
            bulks.set(slot, request);
        } catch (IOException e) {
            throw new RuntimeException("Error submitting bulk request to DB.", e);
        }

        return true;
    }

    private boolean isRepeatableBulkOp(Throwable t) {
        if (t instanceof CompletionException) {
            t = t.getCause();
        }

        if (t instanceof ResponseException) {
            return ((ResponseException) t).status() == 429;
        }

        if (t instanceof OpenSearchException) {
            return ((OpenSearchException) t).status() == 429;
        }

        return false;
    }

    private static long estimateSize(PhotonDoc doc) {
        long size = BASE_DOC_BYTES;
        final var geom = doc.getGeometry();
        if (geom != null) {
            size += (long) geom.getNumPoints() * BYTES_PER_GEOMETRY_POINT;
        }
        return size;
    }

    private boolean setRefreshInterval(OpenSearchClient syncClient, String interval) {
        try {
            syncClient.indices().putSettings(s -> s
                    .index(PhotonIndex.NAME)
                    .settings(set -> set.refreshInterval(Time.of(t -> t.time(interval)))));
            return true;
        } catch (IOException | OpenSearchException e) {
            LOGGER.warn("Could not set refresh_interval={} on index {}.", interval, PhotonIndex.NAME, e);
        }

        return false;
    }

    private String readRefreshInterval(OpenSearchClient syncClient) {
        try {
            final var state = syncClient.indices()
                    .getSettings(g -> g.index(PhotonIndex.NAME))
                    .get(PhotonIndex.NAME);
            if (state != null) {
                var interval = findRefreshInterval(state.settings(), 4);
                if (interval != null) {
                    return interval;
                }
            }
        } catch (IOException | OpenSearchException e) {
            LOGGER.warn("Could not read refresh_interval from index {}.", PhotonIndex.NAME, e);
        }

        return OS_DEFAULT_REFRESH_INTERVAL;
    }

    /**
     * OpenSearch can nest the value under {@code index} or another {@code settings} key
     * depending on API version and request options. Walk the known nesting paths and
     * return the first non-null {@code refresh_interval}. The depth bound guards against
     * pathological responses.
     */
    @Nullable
    private static String findRefreshInterval(@Nullable IndexSettings settings, int depth) {
        if (settings == null || depth == 0) {
            return null;
        }
        final Time refresh = settings.refreshInterval();
        if (refresh != null) {
            return refresh.time();
        }
        final String fromIndex = findRefreshInterval(settings.index(), depth - 1);
        return fromIndex != null ? fromIndex : findRefreshInterval(settings.settings(), depth - 1);
    }
}

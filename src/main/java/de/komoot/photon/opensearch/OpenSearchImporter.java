package de.komoot.photon.opensearch;

import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.transport.OpenSearchTransport;

import java.io.IOException;

@NullMarked
public class OpenSearchImporter implements Importer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String OS_DEFAULT_REFRESH_INTERVAL = "1s";
    private static final int PROGRESS_INTERVAL = 50000;
    /**
     * Estimated bytes per geometry coordinate in the GeoJSON encoding (two formatted doubles
     * plus brackets and separators). Deliberately generous so the byte estimate runs high and
     * we flush early rather than risk a 413.
     */
    static final int BYTES_PER_GEOMETRY_POINT = 40;

    /** Generous flat allowance for a document's non-geometry fields plus its bulk action line. */
    static final int BASE_DOC_BYTES = 1024;


    private final OpenSearchClient client;
    @Nullable private String originalRefreshInterval = null;

    private final BulkRequestPool requestPool;
    private BulkRequest.Builder builder = new BulkRequest.Builder();
    private long bytes = 0;
    private static final long maxBulkBytes = 10 * 1024 * 1024;

    private final long startMillis;
    private long counter = 0;
    private boolean hasPrintedNoUpdates = false;

    public OpenSearchImporter(OpenSearchTransport transport, int threads) {
        this.client = new OpenSearchClient(transport);
        this.requestPool = new BulkRequestPool(transport, threads);

        this.startMillis = System.currentTimeMillis();
    }

    public void disableRefresh() {
        if (originalRefreshInterval == null) {
            originalRefreshInterval = readRefreshInterval();
        }

        if (!setRefreshInterval("-1")) {
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
        requestPool.submit(builder.build());
        builder = new BulkRequest.Builder();
        bytes = 0;
    }

    @Override
    public void finish() {
        try {
            if (bytes > 0) {
                flush();
            }
        } finally {
            try {
                requestPool.drain();
            } finally {
                if (originalRefreshInterval != null) {
                    setRefreshInterval(originalRefreshInterval);
                }
            }
        }

        LOGGER.info("Finished import of {} photon documents. (Total processing time: {}s)",
                counter, (System.currentTimeMillis() - startMillis) / 1000);

        try {
            client.indices().refresh(r -> r.index(PhotonIndex.NAME));
        } catch (IOException e) {
            LOGGER.warn("Refresh of database failed", e);
        }
    }

    private static long estimateSize(PhotonDoc doc) {
        long size = BASE_DOC_BYTES;
        final var geom = doc.getGeometry();
        if (geom != null) {
            size += (long) geom.getNumPoints() * BYTES_PER_GEOMETRY_POINT;
        }
        return size;
    }

    private boolean setRefreshInterval(String interval) {
        try {
            client.indices().putSettings(s -> s
                    .index(PhotonIndex.NAME)
                    .settings(set -> set.refreshInterval(Time.of(t -> t.time(interval)))));
            return true;
        } catch (IOException | OpenSearchException e) {
            LOGGER.warn("Could not set refresh_interval={} on index {}.", interval, PhotonIndex.NAME, e);
        }

        return false;
    }

    private String readRefreshInterval() {
        try {
            final var state = client.indices()
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

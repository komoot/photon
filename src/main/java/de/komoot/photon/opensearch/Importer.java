package de.komoot.photon.opensearch;

import de.komoot.photon.PhotonDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.core.BulkRequest;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@NullMarked
public class Importer implements de.komoot.photon.Importer {
    private static final Logger LOGGER = LogManager.getLogger();

    /** Documents per bulk request. */
    private static final int BULK_SIZE = 10000;

    /** Refresh interval restored on the index after import completes. */
    private static final String DEFAULT_REFRESH_INTERVAL = "1s";

    private final OpenSearchClient client;
    private final ExecutorService submitPool;
    private final Semaphore inFlight;
    private final Phaser pending = new Phaser(1);
    private final AtomicReference<@Nullable Throwable> firstFailure = new AtomicReference<>();
    private final AtomicBoolean hasPrintedNoUpdates = new AtomicBoolean(false);
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final boolean disabledRefresh;

    private BulkRequest.Builder builder = new BulkRequest.Builder();
    private int todo = 0;

    public Importer(OpenSearchClient client) {
        this(client, 1, false);
    }

    public Importer(OpenSearchClient client, int maxConcurrentRequests) {
        this(client, maxConcurrentRequests, true);
    }

    private Importer(OpenSearchClient client, int maxConcurrentRequests, boolean tuneRefresh) {
        this.client = client;
        final int threads = Math.max(1, maxConcurrentRequests);
        this.submitPool = Executors.newFixedThreadPool(threads, r -> {
            final Thread t = new Thread(r, "photon-bulk-submit");
            t.setDaemon(true);
            return t;
        });
        this.inFlight = new Semaphore(threads);
        this.disabledRefresh = tuneRefresh && setRefreshInterval("-1");
    }

    @Override
    public void add(Iterable<PhotonDoc> docs) {
        final Throwable f = firstFailure.get();
        if (f != null) {
            throw new RuntimeException("Error inserting new documents", f);
        }

        String placeID = null;
        int objectId = 0;
        for (var doc : docs) {
            if (objectId == 0) {
                placeID = doc.getPlaceId();
            }
            if (placeID == null) {
                if (hasPrintedNoUpdates.compareAndSet(false, true)) {
                    LOGGER.warn("Documents have no place_id. Updates will not be possible.");
                }
                builder.operations(op -> op.create(i -> i.index(PhotonIndex.NAME).document(doc)));
            } else {
                final String uuid = PhotonDoc.makeUid(placeID, objectId++);
                builder.operations(op -> op.create(i -> i.index(PhotonIndex.NAME).id(uuid).document(doc)));
            }
            if (++todo >= BULK_SIZE) {
                flush();
            }
        }
    }

    @Override
    public void finish() {
        // Idempotent: ImportThread.finish() invokes us defensively after thread.join(),
        // so a normal-path call from the consumer thread plus the defensive call must
        // collapse to a single execution.
        if (!finished.compareAndSet(false, true)) {
            return;
        }

        try {
            if (todo > 0) {
                flush();
            }
        } catch (Throwable t) {
            firstFailure.compareAndSet(null, t);
        } finally {
            // Drain any submissions already handed to the pool before returning, even if
            // the final flush() threw — otherwise background bulk writes could still be
            // hitting the index after finish() returns. arriveAndAwaitAdvance() does not
            // throw InterruptedException.
            pending.arriveAndAwaitAdvance();
            submitPool.shutdown();
        }

        // Restore refresh_interval before surfacing any failure — the index is left in a
        // sane state even when the import aborts, so a subsequent serve is not stuck with
        // refresh disabled.
        if (disabledRefresh) {
            setRefreshInterval(DEFAULT_REFRESH_INTERVAL);
        }

        final Throwable failure = firstFailure.get();
        if (failure != null) {
            LOGGER.error("Import failed.", failure);
            throw new RuntimeException("Error inserting new documents", failure);
        }

        try {
            client.indices().refresh(r -> r.index(PhotonIndex.NAME));
        } catch (IOException e) {
            LOGGER.warn("Refresh of database failed", e);
        }
    }

    /**
     * Hand the current bulk off to a submitter thread and reset the builder.
     * <p>
     * The {@link #inFlight} semaphore caps concurrent in-flight bulks to the configured
     * {@code maxConcurrentRequests}, providing backpressure: when all permits are held,
     * this call blocks until one is released. Retry-on-429 happens synchronously on the
     * submitter thread via {@link BulkRetryHelper#sendWithRetry}, so a retrying bulk
     * continues to hold its permit — no separate retry scheduler needed.
     */
    private void flush() {
        final BulkRequest req = builder.build();
        builder = new BulkRequest.Builder();
        todo = 0;

        try {
            inFlight.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Import interrupted while waiting for a submit slot", e);
        }
        pending.register();
        submitPool.execute(() -> {
            try {
                BulkRetryHelper.sendWithRetry(client, req);
            } catch (RuntimeException e) {
                firstFailure.compareAndSet(null, e);
            } finally {
                inFlight.release();
                pending.arriveAndDeregister();
            }
        });
    }

    /**
     * Apply {@code index.refresh_interval} to the photon index. Returns true if the
     * setting was accepted; false on failure (in which case we carry on with whatever
     * interval the index already had).
     */
    private boolean setRefreshInterval(String interval) {
        try {
            client.indices().putSettings(s -> s
                    .index(PhotonIndex.NAME)
                    .settings(set -> set.refreshInterval(Time.of(t -> t.time(interval)))));
            return true;
        } catch (IOException e) {
            LOGGER.warn("Could not set refresh_interval={} on index {}.", interval, PhotonIndex.NAME, e);
            return false;
        }
    }
}

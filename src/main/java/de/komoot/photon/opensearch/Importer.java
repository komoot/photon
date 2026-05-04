package de.komoot.photon.opensearch;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@NullMarked
public class Importer implements de.komoot.photon.Importer {
    private static final Logger LOGGER = LogManager.getLogger();

    /** Documents per bulk request. */
    private static final int BULK_SIZE = 10000;

    /** Fallback refresh_interval if the original value cannot be read. Matches the OpenSearch default. */
    private static final String FALLBACK_REFRESH_INTERVAL = "1s";

    private final OpenSearchClient client;
    private final BlockingQueue<BulkRequest> bulkQueue;
    // Compared by reference identity in submitLoop. The dummy delete is only there so
    // BulkRequest.Builder.build() passes its required-operations check.
    private final BulkRequest poison = new BulkRequest.Builder()
            .operations(op -> op.delete(d -> d.index(PhotonIndex.NAME).id("__photon_poison_pill__")))
            .build();
    private final List<Thread> submitThreads;
    private final AtomicReference<@Nullable Throwable> firstFailure = new AtomicReference<>();
    private final AtomicBoolean hasPrintedNoUpdates = new AtomicBoolean(false);
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final boolean shouldRestoreRefresh;
    @Nullable private final String originalRefreshInterval;

    // One accumulator per producer thread keeps add() lock-free across the
    // multi-reader import path. Cross-thread handoff happens only at bulkQueue.put().
    private final ConcurrentMap<Thread, Bulk> bulks = new ConcurrentHashMap<>();

    private static final class Bulk {
        BulkRequest.Builder builder = new BulkRequest.Builder();
        int todo = 0;
    }

    public Importer(OpenSearchClient client, int maxConcurrentRequests, boolean tuneRefresh) {
        this.client = client;
        final int threads = Math.max(1, maxConcurrentRequests);
        // One slot keeps a fast submitter from idling between builds. Larger buffers
        // multiply the live-bulk footprint and OOM the embedded OpenSearch under load.
        this.bulkQueue = new ArrayBlockingQueue<>(1);
        this.submitThreads = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            final Thread t = new Thread(this::submitLoop, "photon-bulk-submit-" + i);
            t.setDaemon(true);
            this.submitThreads.add(t);
            t.start();
        }
        // Read before overwrite: the operator's value may not be the OS default.
        this.originalRefreshInterval = tuneRefresh ? readRefreshInterval() : null;
        this.shouldRestoreRefresh = tuneRefresh && setRefreshInterval("-1");
    }

    @Override
    public void add(Iterable<PhotonDoc> docs) {
        final Throwable f = firstFailure.get();
        if (f != null) {
            throw new RuntimeException("Error inserting new documents", f);
        }

        final Bulk b = bulks.computeIfAbsent(Thread.currentThread(), k -> new Bulk());

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
                b.builder.operations(op -> op.create(i -> i.index(PhotonIndex.NAME).document(doc)));
            } else {
                final String uuid = PhotonDoc.makeUid(placeID, objectId++);
                b.builder.operations(op -> op.create(i -> i.index(PhotonIndex.NAME).id(uuid).document(doc)));
            }
            if (++b.todo >= BULK_SIZE) {
                flush(b);
            }
        }
    }

    @Override
    public void finish() {
        if (!finished.compareAndSet(false, true)) {
            return;
        }

        try {
            // Safe to iterate without locking: finish() runs after all producer threads
            // have joined, so no concurrent put into `bulks` is in flight.
            for (Bulk b : bulks.values()) {
                if (b.todo > 0) {
                    flush(b);
                }
            }
        } catch (Throwable t) {
            firstFailure.compareAndSet(null, t);
        } finally {
            // Always drain submitters, even if flush() threw, so no bulks are still
            // in flight after finish() returns.
            shutdownSubmitters();
        }

        // Restore before surfacing failure: a subsequent serve must not be stuck with
        // refresh disabled. The cluster-default case can't round-trip through the typed
        // API, so fall back to "1s".
        if (shouldRestoreRefresh) {
            setRefreshInterval(originalRefreshInterval != null ? originalRefreshInterval : FALLBACK_REFRESH_INTERVAL);
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

    // Retry-on-429 runs on the submitter thread, keeping the worker busy for the
    // duration. That's intentional: it slows the producer under sustained 429s
    // instead of piling on more concurrent bulks.
    private void flush(Bulk b) {
        final BulkRequest req = b.builder.build();
        b.builder = new BulkRequest.Builder();
        b.todo = 0;

        try {
            bulkQueue.put(req);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Import interrupted while waiting for a submit slot", e);
        }
    }

    private void submitLoop() {
        while (true) {
            final BulkRequest req;
            try {
                req = bulkQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (req == poison) {
                return;
            }
            // Once any submitter has recorded a hard failure, drop subsequent bulks
            // without sending. Still drain the queue so producers don't block on put()
            // until they hit firstFailure on their next add().
            if (firstFailure.get() != null) {
                continue;
            }
            try {
                BulkRetryHelper.sendWithRetry(client, req);
            } catch (Throwable t) {
                // Errors (OOM, AssertionError, ...) too: a thread that died silently would
                // hang -j 1 (no consumer to drain the queue) or drop in-flight docs at -j N.
                firstFailure.compareAndSet(null, t);
            }
        }
    }

    private void shutdownSubmitters() {
        for (int i = 0; i < submitThreads.size(); i++) {
            try {
                bulkQueue.put(poison);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Don't leave un-poisoned workers blocked on take(); the join below would hang.
                for (Thread t : submitThreads) t.interrupt();
                break;
            }
        }
        for (Thread t : submitThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
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
        } catch (IOException | OpenSearchException e) {
            LOGGER.warn("Could not set refresh_interval={} on index {}.", interval, PhotonIndex.NAME, e);
            return false;
        }
    }

    /**
     * Read the current {@code index.refresh_interval} for the photon index, or null if it
     * is unset (the index is using the cluster default). Returns null on read failure too;
     * the caller treats that the same as "unknown, use the fallback on restore".
     */
    @Nullable
    private String readRefreshInterval() {
        try {
            final var state = client.indices()
                    .getSettings(g -> g.index(PhotonIndex.NAME))
                    .get(PhotonIndex.NAME);
            return state == null ? null : findRefreshInterval(state.settings(), 4);
        } catch (IOException | OpenSearchException e) {
            LOGGER.warn("Could not read refresh_interval from index {}.", PhotonIndex.NAME, e);
            return null;
        }
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

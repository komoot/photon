package de.komoot.photon.nominatim;

import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Worker thread(s) for bulk importing data into a Photon database.
 */
@NullMarked
public class ImportThread {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PROGRESS_INTERVAL = 50000;
	private static final int MAX_QUEUE_SIZE = 10_000;
    private final BlockingQueue<Iterable<PhotonDoc>> documents = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    private final AtomicLong counter = new AtomicLong();
    private final List<Thread> threads;
    private final long startMillis;
    private volatile boolean producerDone = false;
    private volatile boolean exceptionInThread = false;

    public ImportThread(Importer importer) {
        this(List.of(importer));
    }

    public ImportThread(List<Importer> importers) {
        this.threads = new ArrayList<>(importers.size());
        for (var imp : importers) {
            var thread = new Thread(new ImportRunnable(imp));
            thread.setUncaughtExceptionHandler((t, ex) -> {
                LOGGER.error("Import error.", ex);
                exceptionInThread = true;
            });
            thread.start();
            this.threads.add(thread);
        }
        this.startMillis = System.currentTimeMillis();
    }

    /**
     * Adds the given document from Nominatim to the import queue.
     *
     * @param docs Fully filled nominatim document.
     */
    public void addDocument(@Nullable Iterable<PhotonDoc> docs) {
        if (exceptionInThread) {
            throw new RuntimeException("Import thread failed.");
        }

        if (docs == null || !docs.iterator().hasNext()) {
            return;
        }

        while (true) {
            try {
                documents.put(docs);
                break;
            } catch (InterruptedException e) {
                LOGGER.warn("Thread interrupted while placing document in queue.");
                // Restore interrupted state.
                Thread.currentThread().interrupt();
            }
        }

        final long count = counter.incrementAndGet();
        if (count % PROGRESS_INTERVAL == 0) {
            final double documentsPerSecond = 1000d * count / (System.currentTimeMillis() - startMillis);
            LOGGER.info("Imported {} documents [{}/second]", count, documentsPerSecond);
        }
    }

    /**
     * Finalize the import.
     * Signals consumer threads to stop and waits for them to complete.
     */
    public void finish() {
        producerDone = true;
        for (var thread : threads) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException e) {
                    LOGGER.warn("Thread interrupted while waiting for import thread.");
                    // Restore interrupted state.
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (!exceptionInThread) {
            LOGGER.info("Finished import of {} photon documents. (Total processing time: {}s)",
                    counter.longValue(), (System.currentTimeMillis() - startMillis) / 1000);
        }
    }

    private class ImportRunnable implements Runnable {
        private final Importer importer;

        ImportRunnable(Importer importer) {
            this.importer = importer;
        }

        @Override
        public void run() {
            List<Iterable<PhotonDoc>> batch = new ArrayList<>(MAX_QUEUE_SIZE);
            while (true) {
                if (documents.drainTo(batch) == 0) {
                    if (producerDone && documents.isEmpty()) {
                        break;
                    }
                    try {
                        var item = documents.poll(100, TimeUnit.MILLISECONDS);
                        if (item != null) {
                            batch.add(item);
                        } else {
                            continue;
                        }
                    } catch (InterruptedException e) {
                        LOGGER.info("Interrupted exception", e);
                        // Restore interrupted state.
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                for (Iterable<PhotonDoc> docs : batch) {
                    importer.add(docs);
                }
                batch.clear();
            }
            importer.finish();
        }
    }

}

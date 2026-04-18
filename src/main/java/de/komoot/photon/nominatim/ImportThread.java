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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Worker thread for bulk importing data from a Nominatim database.
 */
@NullMarked
public class ImportThread {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PROGRESS_INTERVAL = 50000;
	private static final int MAX_QUEUE_SIZE = 10_000;
    private static final List<PhotonDoc> FINAL_DOCUMENT = List.of();
    private final BlockingQueue<Iterable<PhotonDoc>> documents = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    private final AtomicLong counter = new AtomicLong();
    private final Importer importer;
    private final Thread thread;
    private final long startMillis;
    private volatile boolean exceptionInThread = false;

    /** True after finish() if any error occurred during the import. */
    public boolean hasErrors() {
        return exceptionInThread;
    }

    public ImportThread(Importer importer) {
        this.importer = importer;
        this.thread = new Thread(new ImportRunnable());
        this.thread.setUncaughtExceptionHandler((t, ex) -> {
            LOGGER.error("Import error.", ex);
            exceptionInThread = true;
        });
        this.thread.start();
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

        if (counter.incrementAndGet() % PROGRESS_INTERVAL == 0) {
            final double documentsPerSecond = 1000d * counter.longValue() / (System.currentTimeMillis() - startMillis);
            LOGGER.info("Imported {} documents [{}/second]", counter.longValue(), documentsPerSecond);
        }
    }

    /**
     * Finalize the import.
     * Sends an end marker to the import thread and then waits for it to join.
     */
    public void finish() {
        while (true) {
            try {
                documents.put(FINAL_DOCUMENT);
                thread.join();
                break;
            } catch (InterruptedException e) {
                LOGGER.warn("Thread interrupted while placing document in queue.");
                // Restore interrupted state.
                Thread.currentThread().interrupt();
            }
        }
        // Defensive: the consumer thread may have died before processing FINAL_DOCUMENT
        // (uncaught exception in importer.add). Importer.finish() is idempotent.
        try {
            importer.finish();
        } catch (Throwable t) {
            LOGGER.error("Importer finish failed.", t);
            exceptionInThread = true;
        }
        if (!exceptionInThread) {
            LOGGER.info("Finished import of {} photon documents. (Total processing time: {}s)",
                    counter.longValue(), (System.currentTimeMillis() - startMillis) / 1000);
        }
    }

    private class ImportRunnable implements Runnable {

        @Override
        public void run() {
            List<Iterable<PhotonDoc>> batch = new ArrayList<>(MAX_QUEUE_SIZE);
            while (true) {
                if (documents.drainTo(batch) == 0) {
                    try {
                        batch.add(documents.take());
                    } catch (InterruptedException e) {
                        LOGGER.info("Interrupted exception", e);
                        // Restore interrupted state.
                        Thread.currentThread().interrupt();
                    }
                }
                for (Iterable<PhotonDoc> docs : batch) {
                    if (docs == FINAL_DOCUMENT) {
                        // importer.finish() is invoked from ImportThread.finish() on the
                        // calling thread, so it runs even if this consumer thread dies.
                        return;
                    }
                    importer.add(docs);
                }
                batch.clear();
            }
        }
    }

}

package de.komoot.photon.nominatim;

import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Worker thread for bulk importing data from a Nominatim database.
 */
@NullMarked
public class ImportThread {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PROGRESS_INTERVAL = 50000;
    private static final List<PhotonDoc> FINAL_DOCUMENT = List.of();
    private final BlockingQueue<Iterable<PhotonDoc>> documents = new LinkedBlockingDeque<>(100);
    private final AtomicLong counter = new AtomicLong();
    private final Importer importer;
    private final Thread thread;
    private final long startMillis;
    private volatile boolean exceptionInThread = false;

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
        if (!exceptionInThread) {
            LOGGER.info("Finished import of {} photon documents. (Total processing time: {}s)",
                    counter.longValue(), (System.currentTimeMillis() - startMillis) / 1000);
        }
    }

    private class ImportRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    final var docs = documents.take();
                    if (!docs.iterator().hasNext()) {
                        break;
                    }
                    importer.add(docs);
                } catch (InterruptedException e) {
                    LOGGER.info("Interrupted exception", e);
                    // Restore interrupted state.
                    Thread.currentThread().interrupt();
                }
            }
            importer.finish();
        }
    }

}

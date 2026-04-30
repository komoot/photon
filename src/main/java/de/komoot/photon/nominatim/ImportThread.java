package de.komoot.photon.nominatim;

import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thin wrapper that forwards documents to the importer on the caller's thread and
 * logs progress. Backpressure flows from the importer's bulk pipeline.
 */
@NullMarked
public class ImportThread {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PROGRESS_INTERVAL = 50000;

    private final Importer importer;
    private final long startMillis;
    // Multiple reader threads share one ImportThread, so counter and the error
    // flag need cross-thread visibility.
    private final AtomicLong counter = new AtomicLong();
    private volatile boolean exceptionInThread = false;

    public boolean hasErrors() {
        return exceptionInThread;
    }

    public ImportThread(Importer importer) {
        this.importer = importer;
        this.startMillis = System.currentTimeMillis();
    }

    public void addDocument(@Nullable Iterable<PhotonDoc> docs) {
        if (exceptionInThread) {
            throw new RuntimeException("Import thread failed.");
        }

        if (docs == null || !docs.iterator().hasNext()) {
            return;
        }

        try {
            importer.add(docs);
        } catch (RuntimeException e) {
            exceptionInThread = true;
            LOGGER.error("Import error.", e);
            throw e;
        }

        final long c = counter.incrementAndGet();
        if (c % PROGRESS_INTERVAL == 0) {
            final double documentsPerSecond = 1000d * c / (System.currentTimeMillis() - startMillis);
            LOGGER.info("Imported {} documents [{}/second]", c, documentsPerSecond);
        }
    }

    public void finish() {
        try {
            importer.finish();
        } catch (Throwable t) {
            LOGGER.error("Importer finish failed.", t);
            exceptionInThread = true;
        }
        if (!exceptionInThread) {
            LOGGER.info("Finished import of {} photon documents. (Total processing time: {}s)",
                    counter.get(), (System.currentTimeMillis() - startMillis) / 1000);
        }
    }
}

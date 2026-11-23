package de.komoot.photon.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DocumentCounter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int PROGRESS_INTERVAL = 50000;

    private final long startMillis;
    private long counter = 0;

    public DocumentCounter() {
        this.startMillis = System.currentTimeMillis();
    }

    public void inc() {
        if (++counter % PROGRESS_INTERVAL == 0) {
            final double documentsPerSecond = 1000d * counter / (System.currentTimeMillis() - startMillis);
            LOGGER.info("Imported {} documents [{}/second]", counter, documentsPerSecond);
        }
    }

    public void finish() {
        LOGGER.info("Finished import of {} photon documents. (Total processing time: {}s)",
                counter, (System.currentTimeMillis() - startMillis) / 1000);
    }
}

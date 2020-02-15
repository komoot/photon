package de.komoot.photon;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

/**
 * Report import progress.
 *
 * @author holger
 */
@Slf4j
public class ImportProgressMonitor {

    private static final int PROGRESS_INTERVAL = 50000;
    
    private final AtomicLong counter = new AtomicLong();
    private long startMillis;

    public void start() {
        startMillis = System.currentTimeMillis();
    }

    public void progressByOne() {
        if (counter.incrementAndGet() % PROGRESS_INTERVAL == 0) {
            reportProgress();
        }
    }

    public void finish() {
        reportProgress();
    }

    private void reportProgress() {
        final double documentsPerSecond = 1000d * counter.longValue()
                        / (System.currentTimeMillis() - startMillis);
        log.info(String.format("imported %s documents [%.1f/second]",
                        MessageFormat.format("{0}", counter.longValue()), documentsPerSecond));
    }
}
package de.komoot.photon.nominatim;

import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
class ImportThread {
    private static final int PROGRESS_INTERVAL = 50000;
    private static final NominatimResult FINAL_DOCUMENT = new NominatimResult(new PhotonDoc(0, null, 0, null, null));
    private final BlockingQueue<NominatimResult> documents = new LinkedBlockingDeque<>(20);
    private final AtomicLong counter = new AtomicLong();
    private final Importer importer;
    private final Thread thread;
    private final long startMillis;

    public ImportThread(Importer importer) {
        this.importer = importer;
        this.thread = new Thread(new ImportRunnable());
        this.thread.start();
        this.startMillis = System.currentTimeMillis();
    }

    /**
     * Adds the given document from Nominatim to the import queue.
     *
     * @param docs Fully filled nominatim document.
     */
    public void addDocument(NominatimResult docs) {
        assert docs != null;
        while (true) {
            try {
                documents.put(docs);
                break;
            } catch (InterruptedException e) {
                log.warn("Thread interrupted while placing document in queue.");
                // Restore interrupted state.
                Thread.currentThread().interrupt();
            }
        }

        if (counter.incrementAndGet() % PROGRESS_INTERVAL == 0) {
            final double documentsPerSecond = 1000d * counter.longValue() / (System.currentTimeMillis() - startMillis);
            log.info(String.format("imported %d documents [%.1f/second]", counter.longValue(), documentsPerSecond));
        }
    }

    /**
     * Finalize the import.
     *
     * Sends an end marker to the import thread and waiting for it to join.
     */
    public void finish() {
        while (true) {
            try {
                documents.put(FINAL_DOCUMENT);
                thread.join();
                break;
            } catch (InterruptedException e) {
                log.warn("Thread interrupted while placing document in queue.");
                // Restore interrupted state.
                Thread.currentThread().interrupt();
            }
        }
        log.info(String.format("finished import of %d photon documents.", counter.longValue()));
    }

    private class ImportRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    NominatimResult docs = documents.take();
                    if (docs == FINAL_DOCUMENT) {
                        break;
                    }
                    int object_id = 0;
                    for (PhotonDoc doc : docs.getDocsWithHousenumber()) {
                        importer.add(doc, object_id++);
                    }
                } catch (InterruptedException e) {
                    log.info("interrupted exception ", e);
                    // Restore interrupted state.
                    Thread.currentThread().interrupt();
                }
            }
            importer.finish();
        }
    }

}

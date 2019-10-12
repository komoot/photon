package de.komoot.photon.json;

import de.komoot.photon.Connector;
import de.komoot.photon.ImportProgressMonitor;
import de.komoot.photon.elasticsearch.Importer;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Import json dump data.
 *
 * @author holger
 */
@Slf4j
public class JsonDumpConnector implements Connector {

    private BufferedReader reader;
    private final Importer importer;

    public JsonDumpConnector(de.komoot.photon.Importer importer, String filename) {
        if (importer instanceof Importer) {
            this.importer = (Importer) importer;
        } else {
            throw new IllegalArgumentException(String.format(
                    "Only importer of type %1 is supported.", Importer.class.getName()));
        }
        try {
            this.reader = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Json dump file '%1' not found " + filename, e);
        }
    }

    public void readEntireDatabase() {
        if (reader == null) {
            throw new IllegalStateException("Reader was not initialized");
        }

        final ImportProgressMonitor progressMonitor = new ImportProgressMonitor();
        progressMonitor.start();

        String sourceLine;
        try {
            while ((reader.readLine()) != null) {
                // first line is action line, currently we assume / support only indexing
                sourceLine = reader.readLine();
                if (sourceLine != null)
                    importer.add(sourceLine, null);

                progressMonitor.progressByOne();
            }
            progressMonitor.finish();
        } catch (IOException e) {
            log.error("Error importing from json dump file", e);
        }
    }
}

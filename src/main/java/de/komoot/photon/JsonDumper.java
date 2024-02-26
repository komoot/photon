package de.komoot.photon;

import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * useful to create json files that can be used for fast re imports
 */
public class JsonDumper implements Importer {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JsonDumper.class);

    private PrintWriter writer;
    private final String[] languages;
    private final String[] extraTags;

    public JsonDumper(String filename, String[] languages, String[] extraTags) throws FileNotFoundException {
        this.writer = new PrintWriter(filename);
        this.languages = languages;
        this.extraTags = extraTags;
    }

    @Override
    public void add(PhotonDoc doc, int object_id) {
        try {
            writer.println("{\"index\": {}}");
            writer.println(Utils.convert(doc, languages, extraTags).string());
        } catch (IOException e) {
            LOGGER.error("Error writing json file", e);
        }
    }

    @Override
    public void finish() {
        if (writer != null) {
            writer.close();
        }
    }
}

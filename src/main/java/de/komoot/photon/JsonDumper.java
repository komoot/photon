package de.komoot.photon;

import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * useful to create json files that can be used for fast re imports
 *
 * @author christoph
 */
@Slf4j
public class JsonDumper implements Importer {
    private PrintWriter writer;
    private final String[] languages;
    private final String[] extraTags;
    private final boolean allExtraTags;

    public JsonDumper(String filename, String[] languages, String[] extraTags, boolean allExtraTags) throws FileNotFoundException {
        this.writer = new PrintWriter(filename);
        this.languages = languages;
        this.extraTags = extraTags;
        this.allExtraTags = allExtraTags;
    }

    @Override
    public void add(PhotonDoc doc) {
        writer.println("{\"index\": {}}");
        writer.println(Utils.convert(doc, languages, extraTags, allExtraTags).asText());
    }

    @Override
    public void finish() {
        if (writer != null) {
            writer.close();
        }
    }
}

package de.komoot.photon;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.Strings;

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
    private PrintWriter writer = null;
    private final String[] languages;

    public JsonDumper(String filename, String languages) throws FileNotFoundException {
        this.writer = new PrintWriter(filename);
        this.languages = languages.split(",");
    }

    @Override
    public void add(PhotonDoc doc) {
        try {
            writer.println("{\"index\": {}}");
            // writer.println(Utils.convert(doc, this.languages).string());
            // FIXME - Above code did not compile for ES 7 - so added the following fix
            writer.println(Strings.toString(Utils.convert(doc, this.languages)));
        } catch (IOException e) {
            log.error("error writing json file", e);
        }
    }

    @Override
    public void finish() {
        if (writer != null) {
            writer.close();
        }
    }
}

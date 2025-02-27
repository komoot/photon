package de.komoot.photon;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.komoot.photon.opensearch.PhotonDocSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

public class JsonDumper implements Importer {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JsonDumper.class);

    final JsonGenerator generator;

    public JsonDumper(String filename, String[] languages, String[] extraTags) throws IOException {
        final var module = new SimpleModule("PhotonDocSerializer",
                new Version(1, 0, 0, null, null, null));
        module.addSerializer(PhotonDoc.class, new PhotonDocSerializer(languages, extraTags));

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);

        generator = mapper.getFactory().createGenerator(new File(filename), JsonEncoding.UTF8);
    }

    @Override
    public void add(PhotonDoc doc, int objectId) {
        try {
            generator.writeStartObject();
            generator.writeObjectField("id", doc.getUid(objectId));
            generator.writeObjectField("document", doc);
            generator.writeEndObject();
        } catch (IOException e) {
            LOGGER.error("Error writing json file", e);
        }

    }

    @Override
    public void finish() {
        try {
            generator.close();
        } catch (IOException e) {
            LOGGER.warn("Error while closing output file",  e);
        }
    }
}

package de.komoot.photon.searcher;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.Constants;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Format a database result into a Photon GeocodeJson response.
 */
public class GeocodeJsonFormatter implements ResultFormatter {
    private static final String[] KEYS_LANG_UNSPEC = {Constants.OSM_TYPE, Constants.OSM_ID, Constants.OSM_KEY, Constants.OSM_VALUE, Constants.OBJECT_TYPE, Constants.POSTCODE, Constants.HOUSENUMBER, Constants.COUNTRYCODE};
    private static final String[] KEYS_LANG_SPEC = {Constants.NAME, Constants.COUNTRY, Constants.CITY, Constants.DISTRICT, Constants.LOCALITY, Constants.STREET, Constants.STATE, Constants.COUNTY};

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String formatError(String msg) {
        try {
            return mapper.writeValueAsString(Map.of("message", msg));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public String convert(List<PhotonResult> results, String language,
                          boolean withGeometry, boolean withDebugInfo, String queryDebugInfo) throws IOException {
        final var writer = new StringWriter();
        final var gen = mapper.createGenerator(writer);

        if (withDebugInfo) {
            gen.useDefaultPrettyPrinter();
        }

        gen.writeStartObject();
        gen.writeStringField("type", "FeatureCollection");
        gen.writeArrayFieldStart("features");

        for (PhotonResult result : results) {
            gen.writeStartObject();
            gen.writeStringField("type", "Feature");

            gen.writeObjectFieldStart("properties");

            for (String key : KEYS_LANG_UNSPEC) {
                put(gen, key, result.get(key));
            }

            for (String key : KEYS_LANG_SPEC) {
                put(gen, key, result.getLocalised(key, language));
            }

            put(gen, "extent", result.getExtent());

            put(gen, "extra", result.getMap("extra"));

            gen.writeEndObject();

            if (withGeometry && result.getGeometry() != null) {
                gen.writeFieldName("geometry");
                gen.writeRawValue(result.getGeometry());
            } else {
                gen.writeObjectFieldStart("geometry");
                gen.writeStringField("type", "Point");
                gen.writeObjectField("coordinates", result.getCoordinates());
                gen.writeEndObject();
            }

            gen.writeEndObject();
        }

        gen.writeEndArray();

        if (withDebugInfo || queryDebugInfo != null) {
            gen.writeObjectFieldStart("properties");
            if (queryDebugInfo != null) {
                gen.writeFieldName("debug");
                gen.writeRawValue(queryDebugInfo);
            }
            if (withDebugInfo) {
                gen.writeArrayFieldStart("raw_data");
                for (var res : results) {
                    gen.writePOJO(res.getRawData());
                }
                gen.writeEndArray();
            }
            gen.writeEndObject();
        }

        gen.writeEndObject();
        gen.close();
        return writer.toString();
    }


    private void put(JsonGenerator gen, String key, Object value) throws IOException {
        if (value != null) {
            gen.writeObjectField(key, value);
        }
    }
}

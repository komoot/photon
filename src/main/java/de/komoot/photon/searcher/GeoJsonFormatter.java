package de.komoot.photon.searcher;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.opensearch.DocFields;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Format a database result into a Photon GeocodeJson response.
 */
@NullMarked
public class GeoJsonFormatter implements ResultFormatter {
    private static final String[] KEYS_LANG_UNSPEC = {
            GeoJsonFields.OSM_TYPE, GeoJsonFields.OSM_ID,
            GeoJsonFields.OSM_KEY, GeoJsonFields.OSM_VALUE, GeoJsonFields.OBJECT_TYPE,
            GeoJsonFields.POSTCODE, GeoJsonFields.HOUSENUMBER, GeoJsonFields.COUNTRYCODE};
    private static final String[] KEYS_LANG_SPEC = {
            GeoJsonFields.NAME, GeoJsonFields.STREET, GeoJsonFields.LOCALITY,
            GeoJsonFields.DISTRICT, GeoJsonFields.CITY, GeoJsonFields.COUNTY,
            GeoJsonFields.STATE, GeoJsonFields.COUNTRY};

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String formatError(@Nullable String msg) {
        try {
            return mapper.writeValueAsString(
                    Map.of("message", msg == null ? "Unknown error." : msg));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public String convert(List<PhotonResult> results, String language,
                          boolean withGeometry, boolean withDebugInfo,
                          @Nullable String queryDebugInfo) throws IOException {
        final var writer = new StringWriter();

        try (var gen = mapper.createGenerator(writer)) {
            if (withDebugInfo) {
                gen.useDefaultPrettyPrinter();
            }

            gen.writeStartObject();
            gen.writeStringField(GeoJsonFields.GEOJSON_KEY_TYPE, GeoJsonFields.GEOJSON_TYPE_FEATURECOLLECTION);
            gen.writeArrayFieldStart(GeoJsonFields.GEOJSON_KEY_FEATURES);

            for (PhotonResult result : results) {
                gen.writeStartObject();
                gen.writeStringField(GeoJsonFields.GEOJSON_KEY_TYPE, GeoJsonFields.GEOJSON_TYPE_FEATURE);

                gen.writeObjectFieldStart(GeoJsonFields.GEOJSON_KEY_PROPERTIES);

                for (String key : KEYS_LANG_UNSPEC) {
                    put(gen, key, result.get(key));
                }

                for (String key : KEYS_LANG_SPEC) {
                    put(gen, key, result.getLocalised(key, language));
                }

                put(gen, GeoJsonFields.EXTENT, result.getExtent());

                put(gen, GeoJsonFields.EXTRA, result.getMap(DocFields.EXTRA));

                gen.writeEndObject();

                if (withGeometry && result.getGeometry() != null) {
                    gen.writeFieldName(GeoJsonFields.GEOJSON_KEY_GEOMETRY);
                    gen.writeRawValue(result.getGeometry());
                } else {
                    gen.writeObjectFieldStart(GeoJsonFields.GEOJSON_KEY_GEOMETRY);
                    gen.writeStringField(GeoJsonFields.GEOJSON_KEY_TYPE, GeoJsonFields.GEOJSON_GEOMETRY_POINT);
                    gen.writeObjectField("coordinates", result.getCoordinates());
                    gen.writeEndObject();
                }

                gen.writeEndObject();
            }

            gen.writeEndArray();

            if (withDebugInfo || queryDebugInfo != null) {
                gen.writeObjectFieldStart(GeoJsonFields.GEOJSON_KEY_PROPERTIES);
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
        }

        return writer.toString();
    }


    private void put(JsonGenerator gen, String key, @Nullable Object value) throws IOException {
        if (value != null) {
            gen.writeObjectField(key, value);
        }
    }
}

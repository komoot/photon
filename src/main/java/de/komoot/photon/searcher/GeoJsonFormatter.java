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
    private static final String[] NAME_PRECEDENCE = {"housename", "int", "loc", "reg", "alt", "old"};
    private static final List<List<String>> KEYS_LANG_UNSPEC = List.of(
            List.of(DocFields.OSM_TYPE, GeoJsonFields.OSM_TYPE),
            List.of(DocFields.OSM_ID, GeoJsonFields.OSM_ID),
            List.of(DocFields.OSM_KEY, GeoJsonFields.OSM_KEY),
            List.of(DocFields.OSM_VALUE, GeoJsonFields.OSM_VALUE),
            List.of(DocFields.OBJECT_TYPE, GeoJsonFields.OBJECT_TYPE),
            List.of(DocFields.HOUSENUMBER, GeoJsonFields.HOUSENUMBER));
    private static final List<List<String>> KEYS_LANG_SPEC = List.of(
            List.of(DocFields.STREET, GeoJsonFields.STREET),
            List.of(DocFields.LOCALITY, GeoJsonFields.LOCALITY),
            List.of(DocFields.DISTRICT, GeoJsonFields.DISTRICT),
            List.of(DocFields.CITY, GeoJsonFields.CITY),
            List.of(DocFields.COUNTY, GeoJsonFields.COUNTY),
            List.of(DocFields.STATE, GeoJsonFields.STATE),
            List.of(DocFields.COUNTRY, GeoJsonFields.COUNTRY));
    private static final List<List<String>> KEYS_POST_ADDRESS = List.of(
            List.of(DocFields.POSTCODE, GeoJsonFields.POSTCODE),
            List.of(DocFields.COUNTRYCODE, GeoJsonFields.COUNTRYCODE),
            List.of(DocFields.EXTRA, GeoJsonFields.EXTRA));

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

                for (var line : KEYS_LANG_UNSPEC) {
                    put(gen, line.get(1), result.get(line.get(0)));
                }

                put(gen, GeoJsonFields.NAME, result.getLocalised(DocFields.NAME, language, NAME_PRECEDENCE));

                for (var line : KEYS_LANG_SPEC) {
                    put(gen, line.get(1), result.getLocalised(line.get(0), language));
                }

                for (var line : KEYS_POST_ADDRESS) {
                    put(gen, line.get(1), result.get(line.get(0)));
                }

                put(gen, GeoJsonFields.EXTENT, result.getExtent());

                gen.writeEndObject();

                final var geometry = result.get(DocFields.GEOMETRY);
                if (withGeometry && geometry != null) {
                    gen.writeObjectField(GeoJsonFields.GEOJSON_KEY_GEOMETRY, geometry);
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

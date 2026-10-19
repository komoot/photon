package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Format a database result into a Photon GeocodeJson response.
 */
@NullMarked
public class GeoJsonFormatter implements ResultFormatter {
    public static final String[] NAME_PRECEDENCE = {"housename", "int", "loc", "reg", "alt", "old"};
    private static final List<List<String>> KEYS_LANG_UNSPEC = List.of(
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

    @Override
    public String formatError(@Nullable String msg) {
        try {
            return JsonMapper.shared().writeValueAsString(
                    Map.of("message", msg == null ? "Unknown error." : msg));
        } catch (JacksonException e) {
            return "{}";
        }
    }

    @Override
    public String convert(List<PhotonResult> results, String language,
                          @Nullable String fallbackLanguage,
                          boolean withGeometry, boolean withDebugInfo,
                          @Nullable String queryDebugInfo) {
        final var writer = new StringWriter();

        JsonMapper mapper;

        if (withDebugInfo) {
            mapper = JsonMapper.builder()
                    .configure(SerializationFeature.INDENT_OUTPUT, true)
                    .build();
        } else {
            mapper = JsonMapper.shared();
        }

        try (var gen = mapper.createGenerator(writer)) {
            gen.writeStartObject();
            gen.writeStringProperty(GeoJsonFields.GEOJSON_KEY_TYPE, GeoJsonFields.GEOJSON_TYPE_FEATURECOLLECTION);
            gen.writeArrayPropertyStart(GeoJsonFields.GEOJSON_KEY_FEATURES);

            for (PhotonResult result : results) {
                gen.writeStartObject();
                gen.writeStringProperty(GeoJsonFields.GEOJSON_KEY_TYPE, GeoJsonFields.GEOJSON_TYPE_FEATURE);

                gen.writeObjectPropertyStart(GeoJsonFields.GEOJSON_KEY_PROPERTIES);

                var osmType = result.get(DocFields.OSM_TYPE);
                if (osmType != null) {
                    gen.writePOJOProperty(GeoJsonFields.OSM_TYPE, osmType);
                    gen.writePOJOProperty(GeoJsonFields.OSM_ID, result.get(DocFields.OSM_ID));
                }

                for (var line : KEYS_LANG_UNSPEC) {
                    put(gen, line.get(1), result.get(line.get(0)));
                }

                put(gen, GeoJsonFields.NAME, result.getLocalisedWithFallback(DocFields.NAME, language, fallbackLanguage, NAME_PRECEDENCE));

                for (var line : KEYS_LANG_SPEC) {
                    put(gen, line.get(1), result.getLocalisedWithFallback(line.get(0), language, fallbackLanguage));
                }

                for (var line : KEYS_POST_ADDRESS) {
                    put(gen, line.get(1), result.get(line.get(0)));
                }

                put(gen, GeoJsonFields.EXTENT, result.getExtent());

                gen.writeEndObject();

                final var geometry = result.get(DocFields.GEOMETRY);
                if (withGeometry && geometry != null) {
                    gen.writePOJOProperty(GeoJsonFields.GEOJSON_KEY_GEOMETRY, geometry);
                } else {
                    gen.writeObjectPropertyStart(GeoJsonFields.GEOJSON_KEY_GEOMETRY);
                    gen.writeStringProperty(GeoJsonFields.GEOJSON_KEY_TYPE, GeoJsonFields.GEOJSON_GEOMETRY_POINT);
                    gen.writePOJOProperty("coordinates", result.getCoordinates());
                    gen.writeEndObject();
                }

                gen.writeEndObject();
            }

            gen.writeEndArray();

            if (withDebugInfo || queryDebugInfo != null) {
                gen.writeObjectPropertyStart(GeoJsonFields.GEOJSON_KEY_PROPERTIES);
                if (withDebugInfo) {
                    gen.writeArrayPropertyStart("raw_data");
                    for (var res : results) {
                        gen.writePOJO(res.getRawData());
                    }
                    gen.writeEndArray();
                }
                if (queryDebugInfo != null) {
                    gen.writeName("debug");
                    gen.writeRawValue(queryDebugInfo);
                }
                gen.writeEndObject();
            }

            gen.writeEndObject();
        }

        return writer.toString();
    }

    private void put(JsonGenerator gen, String key, @Nullable Object value) {
        if (value != null) {
            gen.writePOJOProperty(key, value);
        }
    }
}

package de.komoot.photon.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;


/**
 * Encapsulates the ES index mapping for the photon index.
 */
@Slf4j
public class IndexMapping {
    private static final ObjectMapper objMapper = new ObjectMapper();

    public static ObjectNode buildMinimalMappings() {
        return objMapper.createObjectNode()
                .put("dynamic", false)
                .putPOJO("_source", objMapper
                        .createObjectNode()
                        .putPOJO("excludes", objMapper
                                .createArrayNode()
                                .add("context.*")
                        )
                );
    }
    public static ObjectNode buildMappings(String[] languages) {
        ObjectNode mappings = buildMinimalMappings();

        ObjectNode propertiesWithLanguages = propertiesWithLanguages(languages);

        ObjectNode basicTextField = objMapper.createObjectNode()
                .put("type", "text")
                .put("index", false);

        ObjectNode basicKeywordField = objMapper.createObjectNode()
                .put("type", "keyword")
                .put("index", true);

        ObjectNode coordinate = objMapper.createObjectNode().put("type", "geo_point");
        ObjectNode importance = objMapper.createObjectNode().put("type", "float");
        ObjectNode osmId = objMapper.createObjectNode().put("type", "long");
        ObjectNode postcode = copyToCollector("default");

        ArrayNode copyToDefault = objMapper.createArrayNode().add("collector.default");

        ObjectNode housenumber = objMapper.createObjectNode()
                .put("type", "text")
                .put("index", true)
                .put("analyzer", "index_housenumber")
                .put("search_analyzer", "standard")
                .putPOJO("copy_to", copyToDefault);

        ObjectNode classification = objMapper.createObjectNode()
                .put("type", "text")
                .put("index", true)
                .put("analyzer", "keyword")
                .put("search_analyzer", "search_classification")
                .putPOJO("copy_to", copyToDefault);

        ObjectNode properties = objMapper.createObjectNode()
                .putPOJO("city", propertiesWithLanguages)
                .putPOJO("context", propertiesWithLanguages)
                .putPOJO("county", propertiesWithLanguages)
                .putPOJO("country", propertiesWithLanguages)
                .putPOJO("state", propertiesWithLanguages)
                .putPOJO("street", propertiesWithLanguages)
                .putPOJO("district", propertiesWithLanguages)
                .putPOJO("locality", propertiesWithLanguages)
                .putPOJO("countrycode", basicTextField)
                .putPOJO("osm_key", basicKeywordField)
                .putPOJO("osm_type", basicTextField)
                .putPOJO("osm_value", basicKeywordField)
                .putPOJO("type", basicKeywordField)
                .putPOJO("place_id", basicTextField)
                .putPOJO("parent_place_id", basicTextField)
                .putPOJO("coordinate", coordinate)
                .putPOJO("importance", importance)
                .putPOJO("osm_id", osmId)
                .putPOJO("name", buildNameField(languages))
                .putPOJO("collector", buildCollectorField(languages))
                .putPOJO("postcode", postcode)
                .putPOJO("housenumber", housenumber)
                .putPOJO("classification", classification);

        return mappings.putPOJO("properties", properties);
    }

    private static ObjectNode copyToCollector(String lang) {
        return objMapper.createObjectNode()
                .put("type", "text")
                .put("index", "false")
                .putPOJO("copy_to", objMapper
                        .createArrayNode()
                        .add(String.format("collector.%s", lang))
                );
    }

    private static ObjectNode copyToCollectorWithRaw(String lang) {
        ObjectNode raw = objMapper.createObjectNode()
                .putPOJO("raw", objMapper
                        .createObjectNode()
                        .put("type", "text")
                        .put("analyzer", "index_raw")
                );
        return copyToCollector(lang).putPOJO("fields", raw);
    }

    private static ObjectNode buildNameField(String[] languages) {
        ArrayNode defaultCopyTo = objMapper.createArrayNode()
                .add("collector.default");

        ObjectNode defaultName = objMapper.createObjectNode()
                .put("type", "text")
                .put("index", false)
                .putPOJO("copy_to", defaultCopyTo);

        ObjectNode properties = objMapper.createObjectNode()
                .putPOJO("alt", copyToCollectorWithRaw("default"))
                .putPOJO("int", copyToCollectorWithRaw("default"))
                .putPOJO("loc", copyToCollectorWithRaw("default"))
                .putPOJO("old", copyToCollectorWithRaw("default"))
                .putPOJO("reg", copyToCollectorWithRaw("default"))
                .putPOJO("housename", copyToCollectorWithRaw("default"));


        for (String language : languages) {
            properties.putPOJO(language, nameToCollector(language));
            defaultCopyTo.add(String.format("name.%s", language));
        }

        properties.putPOJO("default", defaultName);

        return objMapper.createObjectNode().putPOJO("properties", properties);
    }

    private static ObjectNode buildCollectorField(String[] languages) {
        ObjectNode defaultCollector = objMapper.createObjectNode()
                .put("type", "text")
                .put("analyzer", "index_ngram")
                .putPOJO("fields", objMapper.createObjectNode()
                    .putPOJO("raw", objMapper
                            .createObjectNode()
                            .put("type", "text")
                            .put("analyzer", "index_raw")
                    )
                );

        ObjectNode collectorProperties = objMapper.createObjectNode()
                .putPOJO("default", defaultCollector);

        for (String language : languages) {
            collectorProperties.putPOJO(language, nameToCollector(language));
        }

        return objMapper.createObjectNode().putPOJO("properties", collectorProperties);
    }

    private static ObjectNode nameToCollector(String lang) {
        return objMapper.createObjectNode()
                .put("type", "text")
                .put("index", "false")
                .putPOJO("fields", objMapper
                        .createObjectNode()
                        .putPOJO("ngrams", objMapper
                                .createObjectNode()
                                .put("type", "text")
                                .put("analyzer", "index_ngram")
                        )
                        .putPOJO("raw", objMapper
                                .createObjectNode()
                                .put("type", "text")
                                .put("analyzer", "index_raw")
                                .put("search_analyzer", "search_raw")
                        )
                )
                .putPOJO("copy_to", objMapper
                        .createArrayNode()
                        .add(String.format("collector.%s", lang))
                );
    }

    private static ObjectNode propertiesWithLanguages(String[] languages) {
        ObjectNode properties = objMapper.createObjectNode()
                .putPOJO("default", copyToCollector("default"));

        for (String language : languages) {
            properties.putPOJO(language, copyToCollector(language));
        }

        return objMapper.createObjectNode().putPOJO("properties", properties);
    }
}

package de.komoot.photon.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;

/**
 * Encapsulates the ES index mapping for the photon index.
 */
@Slf4j
public class IndexMapping {
    private final JSONObject mappings;

    /**
     * Create a new settings object and initialize it with the index settings
     * from the resources.
     */
    public IndexMapping() {
        final InputStream indexSettings = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("mappings.json");

        mappings = new JSONObject(new JSONTokener(indexSettings));
    }

    public void putMapping(Client client, String indexName, String indexType) {
        client.admin().indices().preparePutMapping(indexName)
                .setType(indexType)
                .setSource(mappings.toString(), XContentType.JSON)
                .execute()
                .actionGet();
    }


    public IndexMapping addLanguages(String[] languages) {
        // define collector json strings
        String copyToCollectorString = "{\"type\":\"text\",\"index\":false,\"copy_to\":[\"collector.{lang}\"]}";
        String nameToCollectorString = "{\"type\":\"text\",\"index\":false,\"fields\":{\"ngrams\":{\"type\":\"text\",\"analyzer\":\"index_ngram\"},\"raw\":{\"type\":\"text\",\"analyzer\":\"index_raw\",\"search_analyzer\":\"search_raw\"}},\"copy_to\":[\"collector.{lang}\"]}";
        String collectorString = "{\"type\":\"text\",\"index\":false,\"fields\":{\"ngrams\":{\"type\":\"text\",\"analyzer\":\"index_ngram\"},\"raw\":{\"type\":\"text\",\"analyzer\":\"index_raw\",\"search_analyzer\":\"search_raw\"}},\"copy_to\":[\"collector.{lang}\"]}";

        JSONObject placeObject = mappings.optJSONObject("place");
        JSONObject propertiesObject = placeObject == null ? null : placeObject.optJSONObject("properties");

        if (propertiesObject == null) {
            log.error("cannot add languages to mapping.json, please double-check the mappings.json or the language values supplied");
            return this;
        }

        for (String lang : languages) {
            // create lang-specific json objects
            JSONObject copyToCollectorObject = new JSONObject(copyToCollectorString.replace("{lang}", lang));
            JSONObject nameToCollectorObject = new JSONObject(nameToCollectorString.replace("{lang}", lang));
            JSONObject collectorObject = new JSONObject(collectorString.replace("{lang}", lang));

            // add language specific tags to the collector
            addToCollector("city", propertiesObject, copyToCollectorObject, lang);
            addToCollector("context", propertiesObject, copyToCollectorObject, lang);
            addToCollector("country", propertiesObject, copyToCollectorObject, lang);
            addToCollector("state", propertiesObject, copyToCollectorObject, lang);
            addToCollector("street", propertiesObject, copyToCollectorObject, lang);
            addToCollector("district", propertiesObject, copyToCollectorObject, lang);
            addToCollector("locality", propertiesObject, copyToCollectorObject, lang);
            addToCollector("name", propertiesObject, nameToCollectorObject, lang);

            // add language specific collector to default for name
            JSONObject name = propertiesObject.optJSONObject("name");
            JSONObject nameProperties = name == null ? null : name.optJSONObject("properties");
            if (nameProperties != null) {
                JSONObject defaultObject = nameProperties.optJSONObject("default");
                JSONArray copyToArray = defaultObject.optJSONArray("copy_to");
                copyToArray.put("name." + lang);
            }

            // add language specific collector
            addToCollector("collector", propertiesObject, collectorObject, lang);
        }

        return this;
    }

    private void addToCollector(String key, JSONObject properties, JSONObject collectorObject, String lang) {
        JSONObject keyObject = properties.optJSONObject(key);
        JSONObject keyProperties = keyObject == null ? null : keyObject.optJSONObject("properties");
        if (keyProperties != null && !keyProperties.has(lang)) {
            keyProperties.put(lang, collectorObject);
        }
    }
}

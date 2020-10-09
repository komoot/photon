package de.komoot.photon.elasticsearch;

import de.komoot.photon.CommandLineArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

@Slf4j
public class CustomServer {
    private TransportClient esClient;
    private Integer shards = null;
    private final String[] languages;
    private String clusterName;
    private String transportAddresses;

    public CustomServer(CommandLineArgs args) {
        this.clusterName = args.getCluster();
        this.languages = args.getLanguages().split(",");
        this.transportAddresses = args.getTransportAddresses();
    }


    public CustomServer start() {
        try {
            Settings.Builder sBuilder = Settings.builder();
            sBuilder.put("cluster.name", "elasticsearch_brew");
            // sBuilder.put("transport.type", "netty4").put("http.type", "netty4").put("http.enabled", "true");
            esClient = new PreBuiltTransportClient(sBuilder.build())
                    // elasticsearch_brew
                    .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public TransportClient getClient() {
        return esClient;
    }

    public void deleteIndex() {
        try {
            this.getClient().admin().indices().prepareDelete("photon").execute().actionGet();
        } catch (IndexNotFoundException e) {
            // ignore
        }
    }

    public void recreateIndex() throws IOException {
        deleteIndex();

        final Client client = this.getClient();
        InputStream mappings = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("mappings.json");
        InputStream index_settings = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("index_settings.json");
        final Charset utf8_charset = Charset.forName("utf-8");

        // HACK - sometime we want to experiment with diff mapping and index setting - this makes easy to
        // experiment (use absolute path for mapping and setting if you want to change without compilation)
        String mappingsFileAbsolutePath = "";
        String indexSettingFileAbsolutePath = "";
        if (!Strings.isEmpty(mappingsFileAbsolutePath) && !Strings.isEmpty(indexSettingFileAbsolutePath)) {
            mappings = new FileInputStream(new File(mappingsFileAbsolutePath));
            index_settings = new FileInputStream(new File(indexSettingFileAbsolutePath));
        }

        String mappingsString = IOUtils.toString(mappings, utf8_charset);
        JSONObject mappingsJSON = new JSONObject(mappingsString);

        // add all langs to the mapping
        mappingsJSON = addLangsToMapping(mappingsJSON);

        JSONObject settings = new JSONObject(IOUtils.toString(index_settings, utf8_charset));
        if (shards != null) {
            settings.put("index", new JSONObject("{ \"number_of_shards\":" + shards + " }"));
        }
        client.admin().indices().prepareCreate("photon").setSettings(settings.toString(), XContentType.JSON).execute().actionGet();
        client.admin().indices().preparePutMapping("photon").setType("place").setSource(mappingsJSON.toString(), XContentType.JSON).execute().actionGet();
        log.info("mapping created: " + mappingsJSON.toString());
    }

    private JSONObject addLangsToMapping(JSONObject mappingsObject) {
        // define collector json strings
        String copyToCollectorString = "{\"type\":\"text\",\"index\":false,\"copy_to\":[\"collector.{lang}\"]}";
        String nameToCollectorString = "{\"type\":\"text\",\"index\":false,\"fields\":{\"ngrams\":{\"type\":\"text\",\"analyzer\":\"index_ngram\"},\"raw\":{\"type\":\"text\",\"analyzer\":\"index_raw\"}},\"copy_to\":[\"collector.{lang}\"]}";
        String collectorString = "{\"type\":\"text\",\"index\":false,\"fields\":{\"ngrams\":{\"type\":\"text\",\"analyzer\":\"index_ngram\"},\"raw\":{\"type\":\"text\",\"analyzer\":\"index_raw\"}},\"copy_to\":[\"collector.{lang}\"]}}},\"street\":{\"type\":\"object\",\"properties\":{\"default\":{\"text\":false,\"type\":\"text\",\"copy_to\":[\"collector.default\"]}";

        JSONObject placeObject = mappingsObject.optJSONObject("place");
        JSONObject propertiesObject = placeObject == null ? null : placeObject.optJSONObject("properties");

        if (propertiesObject != null) {
            for (String lang : languages) {
                // create lang-specific json objects
                JSONObject copyToCollectorObject = new JSONObject(copyToCollectorString.replace("{lang}", lang));
                JSONObject nameToCollectorObject = new JSONObject(nameToCollectorString.replace("{lang}", lang));
                JSONObject collectorObject = new JSONObject(collectorString.replace("{lang}", lang));

                // add language specific tags to the collector
                propertiesObject = addToCollector("city", propertiesObject, copyToCollectorObject, lang);
                propertiesObject = addToCollector("context", propertiesObject, copyToCollectorObject, lang);
                propertiesObject = addToCollector("country", propertiesObject, copyToCollectorObject, lang);
                propertiesObject = addToCollector("state", propertiesObject, copyToCollectorObject, lang);
                propertiesObject = addToCollector("street", propertiesObject, copyToCollectorObject, lang);
                propertiesObject = addToCollector("district", propertiesObject, copyToCollectorObject, lang);
                propertiesObject = addToCollector("locality", propertiesObject, copyToCollectorObject, lang);
                propertiesObject = addToCollector("name", propertiesObject, nameToCollectorObject, lang);

                // add language specific collector to default for name
                JSONObject name = propertiesObject.optJSONObject("name");
                JSONObject nameProperties = name == null ? null : name.optJSONObject("properties");
                if (nameProperties != null) {
                    JSONObject defaultObject = nameProperties.optJSONObject("default");
                    JSONArray copyToArray = defaultObject.optJSONArray("copy_to");
                    copyToArray.put("name." + lang);

                    defaultObject.put("copy_to", copyToArray);
                    nameProperties.put("default", defaultObject);
                    name.put("properties", nameProperties);
                    propertiesObject.put("name", name);
                }

                // add language specific collector
                propertiesObject = addToCollector("collector", propertiesObject, collectorObject, lang);
            }
            placeObject.put("properties", propertiesObject);
            return mappingsObject.put("place", placeObject);
        }

        log.error("cannot add languages to mapping.json, please double-check the mappings.json or the language values supplied");
        return null;
    }

    private JSONObject addToCollector(String key, JSONObject properties, JSONObject collectorObject, String lang) {
        JSONObject keyObject = properties.optJSONObject(key);
        JSONObject keyProperties = keyObject == null ? null : keyObject.optJSONObject("properties");
        if (keyProperties != null) {
            keyProperties.put(lang, collectorObject);
            keyObject.put("properties", keyProperties);
            return properties.put(key, keyObject);
        }
        return properties;
    }

    /**
     * Set the maximum number of shards for the embedded node
     * This typically only makes sense for testing
     *
     * @param shards the maximum number of shards
     * @return this Server instance for chaining
     */
    public CustomServer setMaxShards(int shards) {
        this.shards = shards;
        return this;
    }
}

package de.komoot.photon.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * Encapsulates the ES index settings for the photon index. Adds functions to
 * manipulate and apply the settings.
 *
 */
public class IndexSettings {
    private final JSONObject settings;

    /**
     * Create a new settings object and initialize it with the index settings
     * from the resources.
     */
    public IndexSettings() {
        final InputStream indexSettings = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("index_settings.json");

        settings = new JSONObject(new JSONTokener(indexSettings));
    }

    /**
     * Set the number of shards to use for the index.
     *
     * @param numShards Number of shards to use.
     *
     * @return Return this object for chaining.
     */
    public IndexSettings setShards(Integer numShards) {
        if (numShards != null) {
            settings.put("index", new JSONObject().put("number_of_shards", numShards));
        }

        return this;
    }

    /**
     * Add query-time synonyms to the search analyzer.
     *
     * Synonyms need to be supplied in a simple text file with one synonym entry per line.
     * Synonyms need to be comma-separated. Only single-term synonyms are supported at this
     * time. Spaces in the synonym list are considered a syntax error.
     *
     * @param synonymFile File containing the synonyms.
     *
     * @return This object for chaining.
     */
    public IndexSettings setSynonyms(String synonymFile) throws IOException {
        if (synonymFile == null) {
            return this;
        }

        insertJsonArrayAfter(settings, "/analysis/analyzer/search_ngram/filter", "lowercase", "extra_synonyms");
        insertJsonArrayAfter(settings, "/analysis/analyzer/search_raw/filter", "lowercase", "extra_synonyms");

        BufferedReader br = new BufferedReader(new FileReader(synonymFile));

        JSONArray synonyms = new JSONArray();
        String line;
        while ((line = br.readLine()) != null) {
            if (line.indexOf(' ') >= 0) {
                throw new RuntimeException("Synonym list must not contain any spaces or multi word terms.");
            }
            synonyms.put(line.toLowerCase());
        }

        JSONObject filters = (JSONObject) settings.optQuery("/analysis/filter");
        if (filters == null) {
            throw new RuntimeException("Analyser update: cannot find filter definition");
        }
        filters.put("extra_synonyms", new JSONObject().put("type", "synonym").put("synonyms", synonyms));

        return this;
    }

    /**
     * Create a new index using the current index settings.
     *
     * @param client Client connection to use for creating the index.
     * @param indexName Name of the new index
     */
    public void createIndex(Client client, String indexName) {
        client.admin().indices().prepareCreate(indexName)
                .setSettings(settings.toString(), XContentType.JSON)
                .execute()
                .actionGet();
    }

    /**
     * Update the index settings for an existing index.
     *
     * @param client Client connection to use for creating the index.
     * @param indexName Name of the index to update
     */
    public void updateIndex(Client client, String indexName) {
        client.admin().indices().prepareClose(PhotonIndex.NAME).execute().actionGet();
        client.admin().indices().prepareUpdateSettings(PhotonIndex.NAME).setSettings(settings.toString(), XContentType.JSON).execute().actionGet();
        client.admin().indices().prepareOpen(PhotonIndex.NAME).execute().actionGet();
    }

    /**
     * Insert the given value into the array after the string given by positionString.
     * If the position string is not found, throws a runtime error.
     *
     * @param obj            JSON object to insert into.
     * @param jsonPointer    Path description of the array to insert into.
     * @param positionString Marker string after which to insert.
     * @param value          Value to insert.
     */
    private void insertJsonArrayAfter(JSONObject obj, String jsonPointer, String positionString, String value) {
        JSONArray array = (JSONArray) obj.optQuery(jsonPointer);
        if (array == null) {
            throw new RuntimeException("Analyser update: cannot find JSON array at" + jsonPointer);
        }

        for (int i = 0; i < array.length(); i++) {
            if (positionString.equals(array.getString(i))) {
                array.put(i + 1, value);
                return;
            }
        }

        throw new RuntimeException("Analyser update: cannot find position string " + positionString);
    }
}

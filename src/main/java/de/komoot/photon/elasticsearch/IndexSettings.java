package de.komoot.photon.elasticsearch;

import de.komoot.photon.Utils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
     * Add query-time synonyms and classification terms from a file.
     *
     * Synonyms need to be supplied in a simple text file with one synonym entry per line.
     * Synonyms need to be comma-separated. Only single-term synonyms are supported at this
     * time. Spaces in the synonym list are considered a syntax error.
     *
     * @param synonymFile File containing the synonyms.
     *
     * @return This object for chaining.
     */
    public IndexSettings setSynonymFile(String synonymFile) throws IOException {
        if (synonymFile == null) {
            return this;
        }

        JSONObject synonymConfig = new JSONObject(new JSONTokener(new FileReader(synonymFile)));

        setSearchTimeSynonyms(synonymConfig.optJSONArray("search_synonyms"));
        setClassificationTerms(synonymConfig.optJSONArray("classification_terms"));

        return this;
    }

    public IndexSettings setSearchTimeSynonyms(JSONArray synonyms) {
        if (synonyms != null) {
            insertSynonymFilter("extra_synonyms", synonyms);
        }

        return this;
    }

    public IndexSettings setClassificationTerms(JSONArray terms) {
        if (terms == null) {
            return this;
        }

        // Collect for each term in the list the possible classification expansions.
        Map<String, Set<String>> collector = new HashMap<>();
        for (int i = 0; i < terms.length(); i++) {
            JSONObject descr = terms.getJSONObject(i);

            String classString = Utils.buildClassificationString(descr.getString("key"), descr.getString("value")).toLowerCase();

            if (classString != null) {
                JSONArray jsonTerms = descr.getJSONArray("terms");
                for (int j = 0; j < jsonTerms.length(); j++) {
                    String term = jsonTerms.getString(j).toLowerCase().trim();

                    if (term.length() > 1) {
                        collector.computeIfAbsent(term, k -> new HashSet<>()).add(classString);
                    }
                }
            }
        }

        // Create the final list of synonyms. A term can expand to any classificator or not at all.
        JSONArray synonyms = new JSONArray();
        collector.forEach((term, classificators) ->
            synonyms.put(term + " => " + term + "," + String.join(",", classificators)));

        insertSynonymFilter("classification_synonyms", synonyms);

        return this;
    }

    private void insertSynonymFilter(String filterName, JSONArray synonyms) {
        if (!synonyms.isEmpty()) {
            // Create a filter for the synonyms.
            JSONObject filters = (JSONObject) settings.optQuery("/analysis/filter");
            if (filters == null) {
                throw new RuntimeException("Analyser update: cannot find filter definition");
            }
            filters.put(filterName, new JSONObject().put("type", "synonym").put("synonyms", synonyms));

            // add synonym filter to the search analyzers
            insertJsonArrayAfter("/analysis/analyzer/search_ngram", "filter", "lowercase", filterName);
            insertJsonArrayAfter("/analysis/analyzer/search_raw", "filter", "lowercase", filterName);
        }
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
     * @param jsonPointer    Path description of the array to insert into.
     * @param positionString Marker string after which to insert.
     * @param value          Value to insert.
     */
    private void insertJsonArrayAfter(String jsonPointer, String field, String positionString, String value) {
        JSONObject parent = (JSONObject) settings.optQuery(jsonPointer);
        JSONArray array = parent == null ? null : parent.optJSONArray(field);
        if (array == null) {
            throw new RuntimeException("Analyser update: cannot find JSON array at" + jsonPointer);
        }

        // We can't just insert items, so build a new array instead.
        JSONArray new_array = new JSONArray();
        boolean done = false;
        for (int i = 0; i < array.length(); i++) {
            new_array.put(array.get(i));
            if (!done && positionString.equals(array.getString(i))) {
                new_array.put(value);
                done = true;
            }
        }

        if (!done) {
            throw new RuntimeException("Analyser update: cannot find position string " + positionString);
        }

        parent.put(field, new_array);
    }
}

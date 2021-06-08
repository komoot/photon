package de.komoot.photon.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONObject;
import org.json.JSONTokener;

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
}

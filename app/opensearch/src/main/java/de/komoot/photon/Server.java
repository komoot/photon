package de.komoot.photon;

import de.komoot.photon.opensearch.DBPropertyEntry;
import de.komoot.photon.opensearch.IndexMapping;
import de.komoot.photon.opensearch.IndexSettingBuilder;
import de.komoot.photon.opensearch.PhotonIndex;
import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.searcher.SearchHandler;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Date;

public class Server {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Server.class);

    protected OpenSearchClient esClient;

    public Server(String mainDirectory) {
    }

    public Server start(String clusterName, String[] transportAddresses) {
        if (transportAddresses.length == 0) {
            throw new RuntimeException("OpenSearch-port neds an external OpeSearch instance. Use -transport-addresses.");
        }
        final HttpHost[] hosts = new HttpHost[transportAddresses.length];
        for (int i = 0; i < transportAddresses.length; ++i) {
            final String[] parts = transportAddresses[i].split(":", 2);
            hosts[i] = new HttpHost("http", parts[0],
                                    parts.length > 1 ? Integer.parseInt(parts[1]) : 9200);
        }

        final var transport = ApacheHttpClient5TransportBuilder
                .builder(hosts)
                .setMapper(new JacksonJsonpMapper())
                .build();

        esClient = new OpenSearchClient(transport);

        return this;
    }

    public void waitForReady() throws IOException{
        esClient.cluster().health(h -> h.waitForStatus(HealthStatus.Yellow));
    }

    public void refreshIndexes() throws IOException {
        waitForReady();
        esClient.indices().refresh();
    }

    public void shutdown() {
        // external node only, do nothing
    }

    public DatabaseProperties recreateIndex(String[] languages, Date importDate) throws IOException {
        // delete any existing data
        if (esClient.indices().exists(e -> e.index(PhotonIndex.NAME)).value()) {
            esClient.indices().delete(d -> d.index(PhotonIndex.NAME));
        }

        (new IndexSettingBuilder()).createIndex(esClient, PhotonIndex.NAME);

        (new IndexMapping()).addLanguages(languages).putMapping(esClient, PhotonIndex.NAME);

        var dbProperties = new DatabaseProperties()
                .setLanguages(languages)
                .setImportDate(importDate);
        saveToDatabase(dbProperties);

        return dbProperties;
    }

    public void updateIndexSettings(String synonymFile) throws IOException {
        var dbProperties = new DatabaseProperties();
        loadFromDatabase(dbProperties);

        (new IndexSettingBuilder()).setSynonymFile(synonymFile).updateIndex(esClient, PhotonIndex.NAME);

        if (dbProperties.getLanguages() != null) {
            (new IndexMapping())
                    .addLanguages(dbProperties.getLanguages())
                    .putMapping(esClient, PhotonIndex.NAME);
        }
    }

    public void saveToDatabase(DatabaseProperties dbProperties) throws IOException {
        esClient.index(r -> r
                        .index(PhotonIndex.NAME)
                        .id(PhotonIndex.PROPERTY_DOCUMENT_ID)
                        .document(new DBPropertyEntry(dbProperties))
                        );
    }

    public void loadFromDatabase(DatabaseProperties dbProperties) throws IOException {
        var dbEntry = esClient.get(r -> r
                .index(PhotonIndex.NAME)
                .id(PhotonIndex.PROPERTY_DOCUMENT_ID),
                DBPropertyEntry.class);

        if (!dbEntry.found()) {
            throw new RuntimeException("Cannot access property record. Database too old?");
        }

        if (!DatabaseProperties.DATABASE_VERSION.equals(dbEntry.source().databaseVersion)) {
            LOGGER.error("Database has incompatible version '{}'. Expected: {}",
                         dbEntry.source().databaseVersion, DatabaseProperties.DATABASE_VERSION);
            throw new RuntimeException("Incompatible database.");
        }

        dbProperties.setLanguages(dbEntry.source().languages);
        dbProperties.setImportDate(dbEntry.source().importDate);
    }

    public Importer createImporter(String[] languages, String[] extraTags) {
        return null;
    }

    public Updater createUpdater(String[] languages, String[] extraTags) {
        return null;
    }

    public SearchHandler createSearchHandler(String[] languages, int queryTimeoutSec) {
        return null;
    }

    public ReverseHandler createReverseHandler(int queryTimeoutSec) {
        return null;
    }

}

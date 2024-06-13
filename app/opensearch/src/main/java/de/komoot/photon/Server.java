package de.komoot.photon;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.komoot.photon.opensearch.*;
import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.searcher.StructuredSearchHandler;
import org.apache.hc.core5.http.HttpHost;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class Server {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Server.class);

    protected OpenSearchClient client;
    private OpenSearchRunner runner = null;
    final protected String dataDirectory;

    public Server(String mainDirectory) {
        dataDirectory = new File(mainDirectory, "photon_data").getAbsolutePath();
    }

    public Server start(String clusterName, String[] transportAddresses) {
        HttpHost[] hosts;
        if (transportAddresses.length == 0) {
            hosts = startInternal(clusterName);
        } else {
            hosts = new HttpHost[transportAddresses.length];
            for (int i = 0; i < transportAddresses.length; ++i) {
                final String[] parts = transportAddresses[i].split(":", 2);
                hosts[i] = new HttpHost("http", parts[0],
                        parts.length > 1 ? Integer.parseInt(parts[1]) : 9201);
            }
        }

        final var module = new SimpleModule("PhotonResultDeserializer",
                new Version(1, 0, 0, null, null, null));
        module.addDeserializer(OpenSearchResult.class, new OpenSearchResultDeserializer());

        final var mapper = new JacksonJsonpMapper();
        mapper.objectMapper().registerModule(module);

        final var transport = ApacheHttpClient5TransportBuilder
                .builder(hosts)
                .setMapper(mapper)
                .build();

        client = new OpenSearchClient(transport);

        return this;
    }

    private HttpHost[] startInternal(String clusterName) {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", false);
            settingsBuilder.put("discovery.type", "single-node");
            settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9201");
            settingsBuilder.put("indices.query.bool.max_clause_count", "30000");
        }).build(OpenSearchRunner.newConfigs().basePath(dataDirectory).clusterName(clusterName).numOfNode(1));

        runner.ensureYellow();

        HttpHost[] hosts = new HttpHost[runner.getNodeSize()];

        for (int i = 0; i < runner.getNodeSize(); ++i) {
            hosts[i] = new HttpHost("http", "127.0.0.1", Integer.parseInt(runner.getNode(i).settings().get("http.port")));
        }

        return hosts;
    }

    public void waitForReady() throws IOException {
        client.cluster().health(h -> h.waitForStatus(HealthStatus.Yellow));
    }

    public void refreshIndexes() throws IOException {
        waitForReady();
        client.indices().refresh();
    }

    public void shutdown() {
        if (runner != null) {
            try {
                runner.close();
            } catch (IOException e) {
                LOGGER.error("IO error on closing database", e);
            }
        }
    }

    public DatabaseProperties recreateIndex(String[] languages, Date importDate, boolean supportStructuredQueries) throws IOException {
        // delete any existing data
        if (client.indices().exists(e -> e.index(PhotonIndex.NAME)).value()) {
            client.indices().delete(d -> d.index(PhotonIndex.NAME));
        }

        (new IndexSettingBuilder()).setShards(5).createIndex(client, PhotonIndex.NAME);

        (new IndexMapping(supportStructuredQueries)).addLanguages(languages).putMapping(client, PhotonIndex.NAME);

        var dbProperties = new DatabaseProperties()
                .setLanguages(languages)
                .setSupportStructuredQueries(supportStructuredQueries)
                .setImportDate(importDate);
        saveToDatabase(dbProperties);

        return dbProperties;
    }

    public void updateIndexSettings(String synonymFile) throws IOException {
        var dbProperties = new DatabaseProperties();
        loadFromDatabase(dbProperties);

        (new IndexSettingBuilder()).setSynonymFile(synonymFile).updateIndex(client, PhotonIndex.NAME);

        if (dbProperties.getLanguages() != null) {
            (new IndexMapping(dbProperties.getSupportStructuredQueries()))
                    .addLanguages(dbProperties.getLanguages())
                    .putMapping(client, PhotonIndex.NAME);
        }
    }

    public void saveToDatabase(DatabaseProperties dbProperties) throws IOException {
        client.index(r -> r
                        .index(PhotonIndex.NAME)
                        .id(PhotonIndex.PROPERTY_DOCUMENT_ID)
                        .document(new DBPropertyEntry(dbProperties))
                        );
    }

    public void loadFromDatabase(DatabaseProperties dbProperties) throws IOException {
        var dbEntry = client.get(r -> r
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
        dbProperties.setSupportStructuredQueries(dbEntry.source().supportStructuredQueries);
    }

    public Importer createImporter(String[] languages, String[] extraTags) {
        registerPhotonDocSerializer(languages, extraTags);
        return new de.komoot.photon.opensearch.Importer(client);
    }

    public Updater createUpdater(String[] languages, String[] extraTags) {
        registerPhotonDocSerializer(languages, extraTags);
        return new de.komoot.photon.opensearch.Updater(client);
    }

    public SearchHandler createSearchHandler(String[] languages, int queryTimeoutSec) {
        return new OpenSearchSearchHandler(client, languages, queryTimeoutSec);
    }

    public StructuredSearchHandler createStructuredSearchHandler(String[] languages, int queryTimeoutSec) {
        return new OpenSearchStructuredSearchHandler(client, languages, queryTimeoutSec);
    }

    public ReverseHandler createReverseHandler(int queryTimeoutSec) {
        return new OpenSearchReverseHandler(client, queryTimeoutSec);
    }

    private void registerPhotonDocSerializer(String[] languages, String[] extraTags) {
        final var module = new SimpleModule("PhotonDocSerializer",
                new Version(1, 0, 0, null, null, null));
        module.addSerializer(PhotonDoc.class, new PhotonDocSerializer(languages, extraTags));

        ((JacksonJsonpMapper) client._transport().jsonpMapper()).objectMapper().registerModule(module);
    }
}

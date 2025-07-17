package de.komoot.photon;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.komoot.photon.opensearch.*;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.SimpleSearchRequest;
import de.komoot.photon.query.StructuredSearchRequest;
import de.komoot.photon.searcher.SearchHandler;
import org.apache.hc.core5.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import java.io.File;
import java.io.IOException;

public class Server {
    /**
     * Database version created by new imports with the current code.
     *
     * Format must be: major.minor.patch-dev
     *
     * Increase to next to be released version when the database layout
     * changes in an incompatible way. If it is already at the next released
     * version, increase the dev version.
     */
    public static final String DATABASE_VERSION = "0.3.6-1";

    private static final Logger LOGGER = LogManager.getLogger();

    protected OpenSearchClient client;
    private OpenSearchRunner runner = null;
    protected final String dataDirectory;

    public Server(String mainDirectory) {
        dataDirectory = new File(mainDirectory, "photon_data").getAbsolutePath();
    }

    public void start(String clusterName, String[] transportAddresses) throws IOException {
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

        waitForReady();
    }

    private HttpHost[] startInternal(String clusterName) {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", false);
            settingsBuilder.put("discovery.type", "single-node");
            settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9201");
            settingsBuilder.put("indices.query.bool.max_clause_count", "30000");
        }).build(OpenSearchRunner.newConfigs()
                .basePath(dataDirectory)
                .clusterName(clusterName)
                .numOfNode(1)
        );

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

    public void recreateIndex(DatabaseProperties dbProperties) throws IOException {
        // delete any existing data
        if (client.indices().exists(e -> e.index(PhotonIndex.NAME)).value()) {
            client.indices().delete(d -> d.index(PhotonIndex.NAME));
        }

        new IndexSettingBuilder().setShards(5).createIndex(client, PhotonIndex.NAME);

        new IndexMapping(dbProperties.getSupportStructuredQueries())
                .addLanguages(dbProperties.getLanguages())
                .putMapping(client, PhotonIndex.NAME);

        saveToDatabase(dbProperties);
    }

    public void updateIndexSettings(String synonymFile) throws IOException {
        // This ensures we are on the right version. Do not mess with the
        // database if the version does not fit.
        var dbProperties = loadFromDatabase();

        try {
            (new IndexSettingBuilder()).setSynonymFile(synonymFile).updateIndex(client, PhotonIndex.NAME);
        } catch (OpenSearchException ex) {
            client.shutdown();
            throw new UsageException("Could not install synonyms: " + ex.getMessage());
        }

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
                        .document(dbProperties)
                        );
    }

    public DatabaseProperties loadFromDatabase() throws IOException {
        var dbEntry = client.get(r -> r
                .index(PhotonIndex.NAME)
                .id(PhotonIndex.PROPERTY_DOCUMENT_ID),
                DatabaseProperties.class);

        if (!dbEntry.found()) {
            throw new UsageException("Cannot access property record. Database too old?");
        }

        return dbEntry.source();
    }

    public Importer createImporter(DatabaseProperties dbProperties) {
        registerPhotonDocSerializer(dbProperties);
        return new de.komoot.photon.opensearch.Importer(client);
    }

    public Updater createUpdater(DatabaseProperties dbProperties) {
        registerPhotonDocSerializer(dbProperties);
        return new de.komoot.photon.opensearch.Updater(client);
    }

    public SearchHandler<SimpleSearchRequest> createSearchHandler(String[] languages, int queryTimeoutSec) {
        return new OpenSearchSearchHandler(client, languages, queryTimeoutSec);
    }

    public SearchHandler<StructuredSearchRequest> createStructuredSearchHandler(String[] languages, int queryTimeoutSec) {
        return new OpenSearchStructuredSearchHandler(client, languages, queryTimeoutSec);
    }

    public SearchHandler<ReverseRequest> createReverseHandler(int queryTimeoutSec) {
        return new OpenSearchReverseHandler(client, queryTimeoutSec);
    }

    private void registerPhotonDocSerializer(DatabaseProperties dbProperties) {
        final var module = new SimpleModule("PhotonDocSerializer",
                new Version(1, 0, 0, null, null, null));
        module.addSerializer(PhotonDoc.class, new PhotonDocSerializer(dbProperties));

        ((JacksonJsonpMapper) client._transport().jsonpMapper()).objectMapper().registerModule(module);
    }
}

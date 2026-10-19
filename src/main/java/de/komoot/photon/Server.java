package de.komoot.photon;

import de.komoot.photon.config.PhotonDBConfig;
import de.komoot.photon.opensearch.*;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.SimpleSearchRequest;
import de.komoot.photon.query.StructuredSearchRequest;
import de.komoot.photon.searcher.SearchHandler;
import org.apache.hc.core5.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.jackson3.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.io.File;
import java.io.IOException;
import java.util.List;

@NullMarked
public class Server {
    /**
     * Database version created by new imports with the current code.
     * <p>
     * Format must be: major.minor.patch-dev
     * <p>
     * Increase to next to be released version when the database layout
     * changes in an incompatible way. If it is already at the next released
     * version, increase the dev version.
     */
    public static final String DATABASE_VERSION = "1.0.0-4";

    private static final Logger LOGGER = LogManager.getLogger();

    final PhotonDocSerializer photonDocSerializer = new PhotonDocSerializer();

    protected OpenSearchClient client;
    @Nullable private OpenSearchRunner runner = null;

    public Server(PhotonDBConfig config, boolean create) throws IOException {
        final File dataDirectory = new File(config.getDataDirectory(), "photon_data");
        if (!create && config.getTransportAddresses().isEmpty()) {
            if (!dataDirectory.isDirectory()) {
                LOGGER.error("Data directory '{}' doesn't exist.", dataDirectory.getAbsolutePath());
                throw new IOException("OpenSearch database not found.");
            }

            final File nodeDirectory = new File(dataDirectory, "node_1");

            if (!nodeDirectory.isDirectory()) {
                LOGGER.error("Data directory '{}' seems to be empty. Are you using an index for OpenSearch?",
                        dataDirectory.getAbsolutePath());
                throw new IOException("OpenSearch database not found.");
            }
        }

        HttpHost[] hosts;
        if (config.getTransportAddresses().isEmpty()) {
            hosts = startInternal(dataDirectory, config.getCluster());
        } else {
            hosts = config.getTransportAddresses().stream()
                    .map(addr -> addr.split(":", 2))
                    .map(parts -> new HttpHost("http", parts[0],
                        parts.length > 1 ? Integer.parseInt(parts[1]) : 9201))
                    .toArray(HttpHost[]::new);
        }

        final var mapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false)
                .addModule(new SimpleModule().addSerializer(photonDocSerializer))
                .build();

        final var transport = ApacheHttpClient5TransportBuilder
                .builder(hosts)
                .setMapper(new JacksonJsonpMapper(mapper))
                .build();

        client = new OpenSearchClient(transport);

        waitForReady();
    }

    private HttpHost[] startInternal(File dataDirectory, String clusterName) {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", false);
            settingsBuilder.put("discovery.type", "single-node");
            settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9201");
            settingsBuilder.put("indices.query.bool.max_clause_count", "30000");
            settingsBuilder.put("index.codec", "best_compression");
        }).build(OpenSearchRunner.newConfigs()
                .basePath(dataDirectory.getAbsolutePath())
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
        client.indices().refresh(r -> r.index(PhotonIndex.NAME));
    }

    public void shutdown() {
        if (runner != null) {
            try {
                LOGGER.info("Shutting down OpenSearch runner");
                runner.close();
            } catch (IOException e) {
                LOGGER.error("IO error on closing database", e);
            }
        }
    }

    public void recreateIndex(DatabaseProperties dbProperties, List<String> normalizationFilters) throws IOException {
        // delete any existing data
        if (client.indices().exists(e -> e.index(PhotonIndex.NAME)).value()) {
            client.indices().delete(d -> d.index(PhotonIndex.NAME));
        }

        new IndexSettingBuilder()
                .setShards(5)
                .createIndex(client, PhotonIndex.NAME, normalizationFilters);

        new IndexMapping(dbProperties.getReverseOnly()).putMapping(client, PhotonIndex.NAME);
        dbProperties.setSynonymFiltersAvailable(true);

        saveToDatabase(dbProperties);
    }

    public void updateIndexSettings(@Nullable ConfigSynonyms synonyms) throws IOException {
        // This ensures we are on the right version. Do not mess with the
        // database if the version does not fit.
        var dbProperties = loadFromDatabase();

        if (dbProperties.getSynonymsInstalled()) {
            dbProperties.setSynonymFiltersAvailable(true);
        }

        if (dbProperties.getSynonymsInstalled() || synonyms != null) {
            if (synonyms != null && !dbProperties.getSynonymFiltersAvailable()) {
                LOGGER.error("""
                        This database cannot add synonyms on the fly.

                        You either need to reimport the database with the current version of Photon
                        or run 'photon serve' once with synonyms enabled using Photon 1.3.0.
                        """);
                throw new UsageException("Database does not support synonyms.");
            }

            try {
                (new IndexSettingBuilder(synonyms)).updateIndex(client, PhotonIndex.NAME);
            } catch (OpenSearchException ex) {
                closeClientQuietly();
                throw new UsageException("Could not install synonyms: " + ex.getMessage());
            }

            dbProperties.setSynonymsInstalled(synonyms != null);
            saveToDatabase(dbProperties);
        }
    }

    private void closeClientQuietly() {
        try {
            client._transport().close();
        } catch (Exception e) {
            LOGGER.warn("Error closing OpenSearch client transport", e);
        }
    }

    public void saveToDatabase(DatabaseProperties dbProperties) throws IOException {
        client.indices().putMapping(m -> m
                .index(PhotonIndex.NAME)
                .meta(PhotonIndex.META_DB_PROPERTIES, JsonData.of(dbProperties)));
    }

    public DatabaseProperties loadFromDatabase() throws IOException {
        var meta = client.indices()
                .getMapping(m -> m.index(PhotonIndex.NAME))
                .get(PhotonIndex.NAME)
                .mappings()
                .meta();

        if (!meta.containsKey(PhotonIndex.META_DB_PROPERTIES)) {
            throw new UsageException("Cannot access property meta data. Database too old?");
        }

        final var mapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        return meta.get(PhotonIndex.META_DB_PROPERTIES).to(DatabaseProperties.class, new JacksonJsonpMapper(mapper));
    }

    public Importer createImporter(DatabaseProperties dbProperties, int maxConcurrentRequests, boolean tuneRefresh) {
        photonDocSerializer.configureFromDBProperties(dbProperties);
        return new de.komoot.photon.opensearch.Importer(client, maxConcurrentRequests, tuneRefresh);
    }

    public Updater createUpdater(DatabaseProperties dbProperties) {
        photonDocSerializer.configureFromDBProperties(dbProperties);
        return new de.komoot.photon.opensearch.Updater(client);
    }

    public SearchHandler<SimpleSearchRequest> createSearchHandler(int queryTimeoutSec, @Nullable ConfigSynonyms synonyms) {
        return new OpenSearchSearchHandler(client, queryTimeoutSec, synonyms);
    }

    public SearchHandler<StructuredSearchRequest> createStructuredSearchHandler(int queryTimeoutSec) {
        return new OpenSearchStructuredSearchHandler(client, queryTimeoutSec);
    }

    public SearchHandler<ReverseRequest> createReverseHandler(int queryTimeoutSec) {
        return new OpenSearchReverseHandler(client, queryTimeoutSec);
    }

    protected OpenSearchClient getClient() {
        return client;
    }
}

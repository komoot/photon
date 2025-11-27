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
import org.opensearch.client.json.JsonData;
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
    public static final String DATABASE_VERSION = "1.0.0-4";

    private static final Logger LOGGER = LogManager.getLogger();

    protected OpenSearchClient client;
    private OpenSearchRunner runner = null;
    protected final File dataDirectory;

    public Server(String mainDirectory) {
        dataDirectory = new File(mainDirectory, "photon_data");
    }

    public void start(String clusterName, String[] transportAddresses, boolean create) throws IOException {
        if (!create && transportAddresses.length == 0) {
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

    public void recreateIndex(DatabaseProperties dbProperties) throws IOException {
        // delete any existing data
        if (client.indices().exists(e -> e.index(PhotonIndex.NAME)).value()) {
            client.indices().delete(d -> d.index(PhotonIndex.NAME));
        }

        new IndexSettingBuilder().setShards(5).createIndex(client, PhotonIndex.NAME);

        new IndexMapping().putMapping(client, PhotonIndex.NAME);

        saveToDatabase(dbProperties);
    }

    public void updateIndexSettings(String synonymFile) throws IOException {
        // This ensures we are on the right version. Do not mess with the
        // database if the version does not fit.
        var dbProperties = loadFromDatabase();

        if (dbProperties.getSynonymsInstalled() || synonymFile != null) {

            try {
                (new IndexSettingBuilder()).setSynonymFile(synonymFile).updateIndex(client, PhotonIndex.NAME);
            } catch (OpenSearchException ex) {
                closeClientQuietly();
                throw new UsageException("Could not install synonyms: " + ex.getMessage());
            }

            dbProperties.setSynonymsInstalled(synonymFile != null);
            saveToDatabase(dbProperties);
        }
    }

    private void closeClientQuietly() {
        if (client != null) {
            try {
                client._transport().close();
            } catch (Exception e) {
                LOGGER.warn("Error closing OpenSearch client transport", e);
            }
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

        return meta.get(PhotonIndex.META_DB_PROPERTIES).to(DatabaseProperties.class);
    }

    public Importer createImporter(DatabaseProperties dbProperties) {
        registerPhotonDocSerializer(dbProperties);
        return new de.komoot.photon.opensearch.Importer(client);
    }

    public Updater createUpdater(DatabaseProperties dbProperties) {
        registerPhotonDocSerializer(dbProperties);
        return new de.komoot.photon.opensearch.Updater(client);
    }

    public SearchHandler<SimpleSearchRequest> createSearchHandler(int queryTimeoutSec) {
        return new OpenSearchSearchHandler(client, queryTimeoutSec);
    }

    public SearchHandler<StructuredSearchRequest> createStructuredSearchHandler(int queryTimeoutSec) {
        return new OpenSearchStructuredSearchHandler(client, queryTimeoutSec);
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

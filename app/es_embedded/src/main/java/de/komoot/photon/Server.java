package de.komoot.photon;

import de.komoot.photon.elasticsearch.*;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.SimpleSearchRequest;
import de.komoot.photon.query.StructuredSearchRequest;
import de.komoot.photon.searcher.SearchHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

/**
 * Helper class to start/stop ElasticSearch node and get ElasticSearch clients.
 */
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

    public static final String PROPERTY_DOCUMENT_ID = "DATABASE_PROPERTIES";

    private static final String BASE_FIELD = "document_properties";
    private static final String FIELD_VERSION = "database_version";
    private static final String FIELD_LANGUAGES = "indexed_languages";
    private static final String FIELD_IMPORT_DATE = "import_date";
    private static final String FIELD_SUPPORT_GEOMETRIES = "support_geometries";

    private Node esNode;

    protected Client esClient;

    private final File esDirectory;

    protected static class MyNode extends Node {
        public MyNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
            super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), classpathPlugins);
        }
    }

    public Server(String mainDirectory) {
        final File photonDirectory = new File(mainDirectory, "photon_data");
        esDirectory = new File(photonDirectory, "elasticsearch");
    }

    public void start(String clusterName, String[] transportAddresses, boolean create) throws IOException {
        if (!create) {
            final File photonDirectory = esDirectory.getParentFile();
            if (!photonDirectory.isDirectory()) {
                LOGGER.error("Data directory '{}' doesn't exist.", photonDirectory.getAbsolutePath());
                throw new IOException("ElasticSearch database not found.");
            }
            if (!esDirectory.isDirectory()) {
                LOGGER.error("Data directory '{}' seems to be empty. Are you using an index for ElasticSearch?",
                        photonDirectory.getAbsolutePath());
                throw new IOException("ElasticSearch database not found.");
            }
        }

        setupDirectories();

        Settings.Builder sBuilder = Settings.builder();
        sBuilder.put("path.home", this.esDirectory.toString());
        sBuilder.put("network.host", "127.0.0.1"); // http://stackoverflow.com/a/15509589/1245622
        sBuilder.put("cluster.name", clusterName);

        if (transportAddresses.length > 0) {
            TransportClient trClient = new PreBuiltTransportClient(sBuilder.build());
            for (String tAddr : transportAddresses) {
                int index = tAddr.indexOf(":");
                if (index >= 0) {
                    int port = Integer.parseInt(tAddr.substring(index + 1));
                    String addrStr = tAddr.substring(0, index);
                    trClient.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(addrStr, port)));
                } else {
                    trClient.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(tAddr, 9300)));
                }
            }

            esClient = trClient;

            LOGGER.info("Started elastic search client connected to {}", String.join(", ", transportAddresses));

        } else {

            try {
                sBuilder.put("transport.type", "netty4").put("http.type", "netty4").put("http.enabled", "true");
                Settings settings = sBuilder.build();
                Collection<Class<? extends Plugin>> lList = new LinkedList<>();
                lList.add(Netty4Plugin.class);
                esNode = new MyNode(settings, lList);
                esNode.start();

                LOGGER.info("Started elastic search node");

                esClient = esNode.client();

            } catch (NodeValidationException e) {
                throw new UsageException("Error while starting elasticsearch server: " + e.getMessage());
            }

        }
        waitForReady();
    }

    public void waitForReady() {
        esClient.admin().cluster().prepareHealth().setWaitForYellowStatus().get();
    }

    public void refreshIndexes() {
        esClient.admin().cluster().prepareHealth().setWaitForYellowStatus().get();
        esClient.admin().indices().prepareRefresh(PhotonIndex.NAME).get();
    }

    /**
     * stops the elasticsearch node
     */
    public void shutdown() {
        try {
            if (esNode != null)
                esNode.close();

            esClient.close();
        } catch (IOException e) {
            LOGGER.info("Error during elasticsearch server shutdown", e);
        }
    }


    private void setupDirectories() throws IOException {
        final File painlessDirectory = new File(esDirectory, "modules/lang-painless");

        // Need this directory to inject painless modules.
        painlessDirectory.mkdirs();
        // copy script directory to ElasticSearch directory
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Files.copy(loader.getResourceAsStream("modules/lang-painless/antlr4-runtime.jarx"),
                new File(painlessDirectory, "antlr4-runtime.jar").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(loader.getResourceAsStream("modules/lang-painless/asm-debug-all.jarx"),
                new File(painlessDirectory, "asm-debug-all.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(loader.getResourceAsStream("modules/lang-painless/lang-painless.jarx"),
                new File(painlessDirectory, "lang-painless.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(loader.getResourceAsStream("modules/lang-painless/plugin-descriptor.properties"),
                new File(painlessDirectory, "plugin-descriptor.properties").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(loader.getResourceAsStream("modules/lang-painless/plugin-security.policy"),
                new File(painlessDirectory, "plugin-security.policy").toPath(), StandardCopyOption.REPLACE_EXISTING);

    }

    public void recreateIndex(DatabaseProperties dbProperties) throws IOException {
        deleteIndex();

        loadIndexSettings().createIndex(esClient, PhotonIndex.NAME);

        createAndPutIndexMapping(dbProperties.getLanguages(), dbProperties.getSupportStructuredQueries());

        saveToDatabase(dbProperties);
    }

    private void createAndPutIndexMapping(String[] languages, boolean supportStructuredQueries)
    {
        if (supportStructuredQueries) {
            throw new UnsupportedOperationException("Structured queries are not supported for elasticsearch-based Photon. Consider to use OpenSearch.");
        }

        new IndexMapping().addLanguages(languages)
                          .putMapping(esClient, PhotonIndex.NAME, PhotonIndex.TYPE);
    }

    public void updateIndexSettings(String synonymFile) throws IOException {
        // Load the settings from the database to make sure it is at the right
        // version. If the version is wrong, we should not be messing with the
        // index.
        DatabaseProperties dbProperties = loadFromDatabase();

        loadIndexSettings().setSynonymFile(synonymFile).updateIndex(esClient, PhotonIndex.NAME);

        // Sanity check: legacy databases don't save the languages, so there is no way to update
        //               the mappings consistently.
        if (dbProperties.getLanguages() != null) {
            this.createAndPutIndexMapping(dbProperties.getLanguages(), false);
        }
    }

    protected IndexSettings loadIndexSettings() {
        return new IndexSettings();
    }

    private void deleteIndex() {
        try {
            esClient.admin().indices().prepareDelete(PhotonIndex.NAME).execute().actionGet();
        } catch (IndexNotFoundException e) {
            // ignore
        }
    }


   /**
     * Save the global properties to the database.
     *
     * The function saves properties available as members and the database version
     * as currently defined in DATABASE_VERSION.
     */
    public void saveToDatabase(DatabaseProperties dbProperties) throws IOException  {
        final XContentBuilder builder = XContentFactory.jsonBuilder().startObject().startObject(BASE_FIELD)
                        .field(FIELD_VERSION, DATABASE_VERSION)
                        .field(FIELD_LANGUAGES, String.join(",", dbProperties.getLanguages()))
                        .field(FIELD_IMPORT_DATE, dbProperties.getImportDate() instanceof Date ? dbProperties.getImportDate().toInstant() : null)
                        .field(FIELD_SUPPORT_GEOMETRIES, Boolean.toString(dbProperties.getSupportGeometries()))
                        .endObject().endObject();

        esClient.prepareIndex(PhotonIndex.NAME, PhotonIndex.TYPE).
                    setSource(builder).setId(PROPERTY_DOCUMENT_ID).execute().actionGet();
    }

    /**
     * Load the global properties from the database.
     *
     * The function first loads the database version and throws an exception if it does not correspond
     * to the version as defined in DATABASE_VERSION.
     *
     * Currently does nothing when the property entry is missing. Later versions with a higher
     * database version will then fail.
     */
    public DatabaseProperties loadFromDatabase() {
        GetResponse response = esClient.prepareGet(PhotonIndex.NAME, PhotonIndex.TYPE, PROPERTY_DOCUMENT_ID).execute().actionGet();

        if (!response.isExists()) {
            throw new UsageException("Cannot find database properties. Your database version is too old. Please reimport.");
        }

        Map<String, String> properties = (Map<String, String>) response.getSource().get(BASE_FIELD);

        if (properties == null) {
            throw new UsageException("Found database properties but no '" + BASE_FIELD +"' field. Database corrupt?");
        }

        final var dbProps = new DatabaseProperties();

        dbProps.setDatabaseVersion(properties.get(FIELD_VERSION));

        final String langString = properties.get(FIELD_LANGUAGES);
        dbProps.setLanguages(langString == null ? null : langString.split(","));

        String importDateString = properties.get(FIELD_IMPORT_DATE);
        dbProps.setImportDate(importDateString == null ? null : Date.from(Instant.parse(importDateString)));

        dbProps.setSupportGeometries(Boolean.parseBoolean(properties.get(FIELD_SUPPORT_GEOMETRIES)));

        return dbProps;
    }

    public Importer createImporter(DatabaseProperties dbProperties) {
        return new de.komoot.photon.elasticsearch.Importer(esClient, dbProperties);
    }

    public Updater createUpdater(DatabaseProperties dbProperties) {
        return new de.komoot.photon.elasticsearch.Updater(esClient, dbProperties);
    }

    public SearchHandler<SimpleSearchRequest> createSearchHandler(String[] languages, int queryTimeoutSec) {
        return new ElasticsearchSearchHandler(esClient, languages, queryTimeoutSec);
    }

    public SearchHandler<StructuredSearchRequest> createStructuredSearchHandler(String[] languages, int queryTimeoutSec) {
        throw new UnsupportedOperationException("Structured queries are not supported for elasticsearch-based Photon. Consider to use OpenSearch.");
    }

    public SearchHandler<ReverseRequest> createReverseHandler(int queryTimeoutSec) {
        return new ElasticsearchReverseHandler(esClient, queryTimeoutSec);
    }
}

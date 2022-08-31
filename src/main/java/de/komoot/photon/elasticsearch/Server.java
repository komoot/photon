package de.komoot.photon.elasticsearch;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.Importer;
import de.komoot.photon.Updater;
import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.searcher.SearchHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
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

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Helper class to start/stop elasticsearch node and get elasticsearch clients
 *
 * @author felix
 */
@Slf4j
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
    private static final String DATABASE_VERSION = "0.3.6-1";
    public static final String PROPERTY_DOCUMENT_ID = "DATABASE_PROPERTIES";

    private static final String BASE_FIELD = "document_properties";
    private static final String FIELD_VERSION = "database_version";
    private static final String FIELD_LANGUAGES = "indexed_languages";

    private Node esNode;

    protected Client esClient;

    private File esDirectory;

    protected static class MyNode extends Node {
        public MyNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
            super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), classpathPlugins);
        }
    }

    public Server(String mainDirectory) {
        try {
            if (SystemUtils.IS_OS_WINDOWS) {
                setupDirectories(new URL("file:///" + mainDirectory));
            } else {
                setupDirectories(new URL("file://" + mainDirectory));
            }
        } catch (Exception e) {
            throw new RuntimeException("Can't create directories: " + mainDirectory, e);
        }
    }

    public Server start(String clusterName, String[] transportAddresses) {
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

            log.info("Started elastic search client connected to " + String.join(", ", transportAddresses));

        } else {

            try {
                sBuilder.put("transport.type", "netty4").put("http.type", "netty4").put("http.enabled", "true");
                Settings settings = sBuilder.build();
                Collection<Class<? extends Plugin>> lList = new LinkedList<>();
                lList.add(Netty4Plugin.class);
                esNode = new MyNode(settings, lList);
                esNode.start();

                log.info("started elastic search node");

                esClient = esNode.client();

            } catch (NodeValidationException e) {
                throw new RuntimeException("Error while starting elasticsearch server", e);
            }

        }
        return this;
    }

    public void waitForReady() {
        esClient.admin().cluster().prepareHealth().setWaitForYellowStatus().get();
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
            throw new RuntimeException("Error during elasticsearch server shutdown", e);
        }
    }


    private void setupDirectories(URL directoryName) throws IOException, URISyntaxException {
        final File mainDirectory = new File(directoryName.toURI());
        final File photonDirectory = new File(mainDirectory, "photon_data");
        this.esDirectory = new File(photonDirectory, "elasticsearch");
        final File pluginDirectory = new File(esDirectory, "plugins");
        final File scriptsDirectory = new File(esDirectory, "config/scripts");
        final File painlessDirectory = new File(esDirectory, "modules/lang-painless");

        for (File directory : new File[]{photonDirectory, esDirectory, pluginDirectory, scriptsDirectory,
                painlessDirectory}) {
            directory.mkdirs();
        }

        // copy script directory to elastic search directory
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Files.copy(loader.getResourceAsStream("modules/lang-painless/antlr4-runtime.jar"),
                new File(painlessDirectory, "antlr4-runtime.jar").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(loader.getResourceAsStream("modules/lang-painless/asm-debug-all.jar"),
                new File(painlessDirectory, "asm-debug-all.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(loader.getResourceAsStream("modules/lang-painless/lang-painless.jar"),
                new File(painlessDirectory, "lang-painless.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(loader.getResourceAsStream("modules/lang-painless/plugin-descriptor.properties"),
                new File(painlessDirectory, "plugin-descriptor.properties").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(loader.getResourceAsStream("modules/lang-painless/plugin-security.policy"),
                new File(painlessDirectory, "plugin-security.policy").toPath(), StandardCopyOption.REPLACE_EXISTING);

    }

    public DatabaseProperties recreateIndex(String[] languages) throws IOException {
        deleteIndex();

        loadIndexSettings().createIndex(esClient, PhotonIndex.NAME);

        new IndexMapping().addLanguages(languages).putMapping(esClient, PhotonIndex.NAME, PhotonIndex.TYPE);

        DatabaseProperties dbProperties = new DatabaseProperties().setLanguages(languages);
        saveToDatabase(dbProperties);

        return dbProperties;
    }

    public void updateIndexSettings(String synonymFile) throws IOException {
        // Load the settings from the database to make sure it is at the right
        // version. If the version is wrong, we should not be messing with the
        // index.
        DatabaseProperties dbProperties = new DatabaseProperties();
        loadFromDatabase(dbProperties);

        loadIndexSettings().setSynonymFile(synonymFile).updateIndex(esClient, PhotonIndex.NAME);

        // Sanity check: legacy databases don't save the languages, so there is no way to update
        //               the mappings consistently.
        if (dbProperties.getLanguages() != null) {
            new IndexMapping()
                    .addLanguages(dbProperties.getLanguages())
                    .putMapping(esClient, PhotonIndex.NAME, PhotonIndex.TYPE);
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
     * The function saved properties available as members and the database version
     * as currently defined in DATABASE_VERSION.
     */
    public void saveToDatabase(DatabaseProperties dbProperties) throws IOException  {
        final XContentBuilder builder = XContentFactory.jsonBuilder().startObject().startObject(BASE_FIELD)
                        .field(FIELD_VERSION, DATABASE_VERSION)
                        .field(FIELD_LANGUAGES, String.join(",", dbProperties.getLanguages()))
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
    public void loadFromDatabase(DatabaseProperties dbProperties) {
        GetResponse response = esClient.prepareGet(PhotonIndex.NAME, PhotonIndex.TYPE, PROPERTY_DOCUMENT_ID).execute().actionGet();

        // We are currently at the database version where versioning was introduced.
        if (!response.isExists()) {
            return;
        }

        Map<String, String> properties = (Map<String, String>) response.getSource().get(BASE_FIELD);

        if (properties == null) {
            throw new RuntimeException("Found database properties but no '" + BASE_FIELD +"' field. Database corrupt?");
        }

        String version = properties.getOrDefault(FIELD_VERSION, "");
        if (!DATABASE_VERSION.equals(version)) {
            log.error("Database has incompatible version '" + version + "'. Expected: " + DATABASE_VERSION);
            throw new RuntimeException("Incompatible database.");
        }

        String langString = properties.get(FIELD_LANGUAGES);
        dbProperties.setLanguages(langString == null ? null : langString.split(","));
    }

    public Importer createImporter(String[] languages, String[] extraTags) {
        return new de.komoot.photon.elasticsearch.Importer(esClient, languages, extraTags);
    }

    public Updater createUpdater(String[] languages, String[] extraTags) {
        return new de.komoot.photon.elasticsearch.Updater(esClient, languages, extraTags);
    }

    public SearchHandler createSearchHandler(String[] languages) {
        return new ElasticsearchSearchHandler(esClient, languages);
    }

    public ReverseHandler createReverseHandler() {
        return new ElasticsearchReverseHandler(esClient);
    }
}

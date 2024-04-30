package de.komoot.photon;

import de.komoot.photon.opensearch.OpenSearchTestServer;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

public class ESBaseTester {
    public static final String TEST_CLUSTER_NAME = "photon-test";
    protected static GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @TempDir
    protected Path dataDirectory;

    private OpenSearchTestServer server;


    protected PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) {
        return null;
    }

    @AfterEach
    public void tearDown() throws IOException {
        shutdownES();
    }

    protected PhotonResult getById(int id) {
        return null;
    }

    public void setUpES() throws IOException {
        setUpES(dataDirectory, "en");
    }

    public void setUpES(Path test_directory, String... languages) throws IOException {
        server = new OpenSearchTestServer(test_directory.toString());
        server.startTestServer(TEST_CLUSTER_NAME);
        server.recreateIndex(languages, new Date());
        server.refreshIndexes();
    }

    protected Importer makeImporter() {
        return null;
    }

    protected Importer makeImporterWithExtra(String... extraTags) {
        return null;
    }

    protected Importer makeImporterWithLanguages(String... languages) {
        return null;
    }

    protected Updater makeUpdater() {
        return null;
    }

    protected Updater makeUpdaterWithExtra(String... extraTags) {
        return null;
    }

    protected Server getServer() {
        assert server != null;

        return server;
    }

    protected void refresh() throws IOException {
        server.refreshIndexes();
    }

    /**
     * Shutdown the ES node
     */
    public void shutdownES() throws IOException {
        if (server != null) {
            server.stopTestServer();
        }
    }

}

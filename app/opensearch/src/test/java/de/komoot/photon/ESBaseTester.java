package de.komoot.photon;

import de.komoot.photon.opensearch.OpenSearchTestServer;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;

public class ESBaseTester {
    public static final String TEST_CLUSTER_NAME = "photon-test";
    protected static GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @TempDir
    protected Path dataDirectory;

    private OpenSearchTestServer server;

    protected PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) {
        final var location = FACTORY.createPoint(new Coordinate(lon, lat));
        return new PhotonDoc(id, "W", osmId, key, value)
                .names(Collections.singletonMap("name", "berlin"))
                .centroid(location);
    }

    @AfterEach
    public void tearDown() throws IOException {
        shutdownES();
    }

    protected PhotonResult getById(int id) {
        return getById(Integer.toString(id));
    }

    protected PhotonResult getById(String id) {
        return server.getByID(id);
    }

    public void setUpES() throws IOException {
        setUpES(dataDirectory, "en");
    }

    public void setUpES(Path test_directory, String... languages) throws IOException {
        server = new OpenSearchTestServer(test_directory.toString());
        server.startTestServer(TEST_CLUSTER_NAME);
        server.recreateIndex(languages, new Date(), true);
        server.refreshIndexes();
    }

    protected Importer makeImporter() {
        return server.createImporter(new String[]{"en"}, new String[]{});
    }

    protected Importer makeImporterWithExtra(String... extraTags) {
        return server.createImporter(new String[]{"en"}, extraTags);
    }

    protected Importer makeImporterWithLanguages(String... languages) {
        return server.createImporter(languages, new String[]{});
    }

    protected Updater makeUpdater() {
        return server.createUpdater(new String[]{"en"}, new String[]{});
    }

    protected Updater makeUpdaterWithExtra(String... extraTags) {
        return server.createUpdater(new String[]{"en"}, extraTags);
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

package de.komoot.photon;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterEach;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Start an ES server with some test data that then can be queried in tests that extend this class
 */
public class ESBaseTester {
    public static final String TEST_CLUSTER_NAME = "photon-test";
    protected static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private ElasticTestServer server;
    final DatabaseProperties dbProperties = new DatabaseProperties();

    protected PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) throws ParseException {
        Point location = FACTORY.createPoint(new Coordinate(lon, lat));
        return new PhotonDoc(id, "W", osmId, key, value).names(Collections.singletonMap("name", "berlin")).centroid(location).geometry(new WKTReader().read("POLYGON ((6.4440619 52.1969454, 6.4441094 52.1969158, 6.4441408 52.1969347, 6.4441138 52.1969516, 6.4440933 52.1969643, 6.4440619 52.1969454))"));
    }

    protected PhotonResult getById(int id) {
        return getById(String.valueOf(id));
    }

    protected PhotonResult getById(String id) {
        return server.getById(id);
    }


    @AfterEach
    public void tearDown() throws IOException {
        shutdownES();
    }

    public void setUpES(Path testDirectory) throws IOException {
        server = new ElasticTestServer(testDirectory.toString());
        server.start(TEST_CLUSTER_NAME, new String[]{});
        server.recreateIndex(dbProperties);
        refresh();
    }

    protected Importer makeImporter() {
        return server.createImporter(dbProperties);
    }

    protected Updater makeUpdater() {
        return server.createUpdater(dbProperties);
    }

    protected ElasticTestServer getServer() {
        if (server == null) {
            throw new RuntimeException("call setUpES before using getClient");
        }

        return server;
    }

    protected void refresh() {
        server.refresh();
    }

    protected DatabaseProperties getProperties() {
        return dbProperties;
    }

    /**
     * Shutdown the ES node
     */
    public void shutdownES() {
        if (server != null)
            server.shutdown();
    }
}

package de.komoot.photon;

import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterEach;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;


public class ESBaseTester {
    public static final String TEST_CLUSTER_NAME = TestServer.TEST_CLUSTER_NAME;
    protected static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private TestServer server;
    private final DatabaseProperties dbProperties = new DatabaseProperties();

    protected PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) {
        final Geometry geom;
        try {
            geom = new WKTReader().read("POLYGON ((6.4440619 52.1969454, 6.4441094 52.1969158, 6.4441408 52.1969347, 6.4441138 52.1969516, 6.4440933 52.1969643, 6.4440619 52.1969454))");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return new PhotonDoc(id, "W", osmId, key, value)
                .names(Map.of("name", "berlin"))
                .centroid(FACTORY.createPoint(new Coordinate(lon, lat)))
                .geometry(geom);
    }

    @AfterEach
    public void tearDown() {
        shutdownES();
    }

    protected PhotonResult getById(int id) {
        return getById(Integer.toString(id));
    }

    protected PhotonResult getById(String id) {
        return server.getByID(id);
    }

    public void setUpES(Path dataDirectory) throws IOException {
        server = new TestServer(dataDirectory.toString());
        server.startTestServer(TEST_CLUSTER_NAME);
        server.recreateIndex(dbProperties);
        server.refreshIndexes();
    }

    protected Importer makeImporter() {
        return server.createImporter(dbProperties);
    }

    protected Updater makeUpdater() {
        return server.createUpdater(dbProperties);
    }

    protected Server getServer() {
        assert server != null;

        return server;
    }

    protected DatabaseProperties getProperties() {
        return dbProperties;
    }

    protected void refresh() {
        server.refreshTestServer();
    }

    /**
     * Shutdown the ES node
     */
    public void shutdownES() {
        if (server != null) {
            server.stopTestServer();
        }
    }

}

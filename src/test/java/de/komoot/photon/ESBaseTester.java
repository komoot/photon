package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.NameMap;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterEach;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


public class ESBaseTester {
    public static final String TEST_CLUSTER_NAME = TestServer.TEST_CLUSTER_NAME;
    protected static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private TestServer server;
    private final DatabaseProperties dbProperties = new DatabaseProperties();

    protected NameMap makeDocNames(String... names) {
        Map<String, String> nameMap = new HashMap<>();

        for (int i = 0; i < names.length - 1; i += 2) {
            nameMap.put(names[i], names[i+1]);
        }

        return NameMap.makeForPlace(nameMap, dbProperties.getLanguages());
    }

    protected Map<String, String> makeAddressNames(String... names) {
        Map<String, String> nameMap = new HashMap<>();

        for (int i = 0; i < names.length - 1; i += 2) {
            nameMap.put(names[i], names[i+1]);
        }

        return AddressRow.make(nameMap, "place", "city", 16, dbProperties.getLanguages()).getName();
    }

    protected Point makePoint(double x, double y) {
        return FACTORY.createPoint(new Coordinate(x, y));
    }

    protected Geometry makeDocGeometry(String wkt) {
        try {
            return new WKTReader().read(wkt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
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

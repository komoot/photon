package de.komoot.photon;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;

/**
 * Start an ES server with some test data that then can be queried in tests that extend this class
 */
public class ESBaseTester {
    @TempDir
    protected Path dataDirectory;

    public static final String TEST_CLUSTER_NAME = "photon-test";
    protected static GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private ElasticTestServer server;

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

    public void setUpES() throws IOException {
        setUpES(dataDirectory, false,"en");
    }

    public void setUpESWithPolygons() throws IOException {
        setUpES(dataDirectory, true,"en");
    }
    /**
     * Setup the ES server
     *
     * @throws IOException
     */
    public void setUpES(Path test_directory, boolean supportPolygons, String... languages) throws IOException {
        server = new ElasticTestServer(test_directory.toString());
        server.start(TEST_CLUSTER_NAME, new String[]{});
        server.recreateIndex(languages, new Date(), false, supportPolygons);
        refresh();
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

    protected ElasticTestServer getServer() {
        if (server == null) {
            throw new RuntimeException("call setUpES before using getClient");
        }

        return server;
    }

    protected void refresh() {
        server.refresh();
    }

    /**
     * Shutdown the ES node
     */
    public void shutdownES() {
        if (server != null)
            server.shutdown();
    }
}

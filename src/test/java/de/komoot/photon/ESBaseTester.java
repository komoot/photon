package de.komoot.photon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import de.komoot.photon.elasticsearch.Server;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetResponse;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * Start an ES server with some test data that then can be queried in tests that extend this class
 *
 * @author Peter Karich
 */
@Slf4j
public class ESBaseTester {
    public static final String TEST_CLUSTER_NAME = "photon-test";
    private static GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private Server server;

    protected PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) {
        Point location = FACTORY.createPoint(new Coordinate(lon, lat));
        return new PhotonDoc(id, "W", osmId, key, value).names(Collections.singletonMap("name", "berlin")).centroid(location);
    }

    protected GetResponse getById(int id) {
        return server.getById(id);
    }


    @AfterEach
    public void tearDown() {
        deleteIndex();
        shutdownES();
    }

    public void setUpES() throws IOException {
        setUpES("en");
    }
    /**
     * Setup the ES server
     *
     * @throws IOException
     */
    public void setUpES(String... languages) throws IOException {
        server = new Server(new File("./target/es_photon_test").getAbsolutePath()).setMaxShards(1).start(TEST_CLUSTER_NAME, new String[]{});
        deleteIndex(); // just in case of an abnormal abort previously
        server.recreateIndex(languages);
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

    protected Server getServer() {
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

    public void deleteIndex() {
        if (server != null)
            server.deleteIndex();
    }
}

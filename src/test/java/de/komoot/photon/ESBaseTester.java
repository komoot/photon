package de.komoot.photon;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import de.komoot.photon.elasticsearch.PhotonIndex;
import de.komoot.photon.elasticsearch.Server;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.junit.After;

import java.io.File;
import java.io.IOException;

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
        ImmutableMap<String, String> nameMap = ImmutableMap.of("name", "berlin");
        Point location = FACTORY.createPoint(new Coordinate(lon, lat));
        return new PhotonDoc(id, "way", osmId, key, value, nameMap, null, null, null, null, 0, 0.5, null, location, 0, 0);
    }


    @After
    public void tearDown() {
        shutdownES();
    }

    /**
     * Setup the ES server
     *
     * @throws IOException
     */
    public void setUpES() throws IOException {
        server = new Server(TEST_CLUSTER_NAME, new File("./target/es_photon_test").getAbsolutePath(), "en", "").setMaxShards(1).start();
        server.recreateIndex();
        refresh();
    }

    protected Client getClient() {
        if (server == null) {
            throw new RuntimeException("call setUpES before using getClient");
        }

        return server.getClient();
    }

    protected void refresh() {
        getClient().admin().indices().refresh(new RefreshRequest(PhotonIndex.NAME)).actionGet();
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

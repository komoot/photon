package de.komoot.photon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import de.komoot.photon.elasticsearch.Importer;
import de.komoot.photon.elasticsearch.Server;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

/**
 * Start an ES server with some test data that then can be queried in tests that extend this class
 *
 * @author Peter Karich
 */
@Slf4j
public class ESBaseTester {

    public final String clusterName = "photon-test";
    private final String indexName = "photon";

    private Server server;

    GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    protected Client client;

    private PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) {
        ImmutableMap<String, String> nameMap = ImmutableMap.of("name", "berlin");
        Point location = FACTORY.createPoint(new Coordinate(lon, lat));
        return new PhotonDoc(id, "way", osmId, key, value, nameMap, null, null, null, 0, 0.5, null, location, 0, 0);
    }

    @Before
    public void setUp() throws Exception {
        setUpES();
        ImmutableList<String> tags = ImmutableList.of("tourism", "attraction", "tourism", "hotel", "tourism", "museum", "tourism", "information", "amenity",
                "parking", "amenity", "restaurant", "amenity", "information", "food", "information", "railway", "station");
        client = getClient();
        Importer instance = new Importer(client, "en", false);
        double lon = 13.38886;
        double lat = 52.51704;
        for (int i = 0; i < tags.size(); i++) {
            String key = tags.get(i);
            String value = tags.get(++i);
            PhotonDoc doc = this.createDoc(lon, lat, i, i, key, value);
            instance.add(doc);
            lon += 0.00004;
            lat += 0.00086;
            doc = this.createDoc(lon, lat, i + 1, i + 1, key, value);
            instance.add(doc);
            lon += 0.00004;
            lat += 0.00086;
        }
        instance.finish();
        refresh();
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
        server = new Server(clusterName, new File("./target/es_photon_test").getAbsolutePath(), "en", "").setMaxShards(1).start();
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
        getClient().admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
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

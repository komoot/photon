package de.komoot.photon;

import de.komoot.photon.opensearch.Importer;
import de.komoot.photon.opensearch.OpenSearchTestServer;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;


public class ESBaseTester {
    public static final String TEST_CLUSTER_NAME = "photon-test";
    protected static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @TempDir
    protected Path dataDirectory;

    private OpenSearchTestServer server;

    protected PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) throws ParseException {
        final var location = FACTORY.createPoint(new Coordinate(lon, lat));
        return new PhotonDoc(id, "W", osmId, key, value)
                .names(Collections.singletonMap("name", "berlin"))
                .centroid(location)
                .geometry(new WKTReader().read("POLYGON ((6.4440619 52.1969454, 6.4441094 52.1969158, 6.4441408 52.1969347, 6.4441138 52.1969516, 6.4440933 52.1969643, 6.4440619 52.1969454))"));
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
        setUpES(dataDirectory, false, "en");
    }

    public void setUpESWithGeometry() throws IOException {
        setUpES(dataDirectory, true, "en");
    }

    public void setUpES(Path testDirectory, boolean supportGeometries, String... languages) throws IOException {
        server = new OpenSearchTestServer(testDirectory.toString());
        server.startTestServer(TEST_CLUSTER_NAME);
        server.recreateIndex(languages, new Date(), true, supportGeometries);
        server.refreshIndexes();
    }

    protected Importer makeImporter() {
        return (Importer) server.createImporter(new String[]{"en"}, new String[]{});
    }

    protected Importer makeImporterWithExtra(String... extraTags) {
        return (Importer) server.createImporter(new String[]{"en"}, extraTags);
    }

    protected Importer makeImporterWithLanguages(String... languages) {
        return (Importer) server.createImporter(languages, new String[]{});
    }

    protected Updater makeUpdater() {
        return (Updater) server.createUpdater(new String[]{"en"}, new String[]{});
    }

    protected Updater makeUpdaterWithExtra(String... extraTags) {
        return (Updater) server.createUpdater(new String[]{"en"}, extraTags);
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

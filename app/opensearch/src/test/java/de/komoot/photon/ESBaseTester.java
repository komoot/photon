package de.komoot.photon;

import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.IOException;
import java.nio.file.Path;

public class ESBaseTester {
    @TempDir
    protected Path dataDirectory;

    public static final String TEST_CLUSTER_NAME = "photon-test";
    protected static GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    protected PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) {
        return null;
    }

    @AfterEach
    public void tearDown() {
        shutdownES();
    }


    protected PhotonResult getById(int id) {
        return null;
    }

    public void setUpES() throws IOException {
        setUpES(dataDirectory, "en");
    }

    public void setUpES(Path test_directory, String... languages) throws IOException {
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
        return null;
    }

    protected void refresh() {
    }

    /**
     * Shutdown the ES node
     */
    public void shutdownES() {
    }

}

package de.komoot.photon.elasticsearch;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.ConfigExtraTags;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.SearchHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticResultTest  extends ESBaseTester {
    @TempDir
    private static Path instanceTestDirectory;

    private Map<String, String> makeMap(String... kv) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            result.put(kv[i], kv[i + 1]);
        }

        return result;
    }

    protected PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) {
        Point location = FACTORY.createPoint(new Coordinate(lon, lat));
        return new PhotonDoc(id, "W", osmId, key, value).centroid(location);
    }


    @BeforeAll
    void setUp() throws Exception {
        setUpES(instanceTestDirectory, false, "en", "de", "fr", "it");
        Importer instance = getServer().createImporter(new String[]{"en", "de", "fr", "it"},
                 new ConfigExtraTags(List.of("population",  "capital")));

        instance.add(List.of(
                createDoc(45.2, -7.45, 123, 1123, "place", "city")
                        .names(makeMap("name", "München", "name:it", "Monacco", "name:en", "Munich"))
                        .address(Collections.singletonMap("state", "Bavaria"))
                        .countryCode("de")
                        .extraTags(makeMap("population", "many", "capital", "yes", "maxage", "99"))));
        instance.add(List.of(
                createDoc(0, 0, 99, 11999, "place", "locality")
                        .names(makeMap("name", "null island"))));
        instance.add(List.of(
                createDoc(-179, 1.0001, 923, 1923, "place", "house")
                        .houseNumber("34")
                        .bbox(FACTORY.createMultiPoint(new Coordinate[]{new Coordinate(-179.5, 1.0), new Coordinate(-178.5, 1.1)}))
                        .address(makeMap("street", "Hauptstr", "city", "Hamburg"))));
        instance.add(List.of(
                new PhotonDoc(42, "N", 42, "place", "hamlet")
                        .names(makeMap("name", "nowhere"))));

        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() throws IOException {
        super.tearDown();
    }

    private PhotonResult search(String query) {
        SearchHandler handler = getServer().createSearchHandler(new String[]{"en", "de", "it"}, 1);

        return handler.search(new PhotonRequest(query, "default")).get(0);
    }


    @Test
    void testGet() {
        PhotonResult result = search("München");

        assertAll("get",
                () -> assertEquals("DE", result.get("countrycode")),
                () -> assertNull(result.get("horse"))
        );
    }

    @Test
    void testGetMap() {
        assertAll("getMap",
                () -> assertEquals(makeMap("default", "München", "en", "Munich", "it", "Monacco"),
                                   search("München").getMap("name")),
                () -> assertEquals(makeMap("default", "null island"),
                                   search("null island").getMap("name")),
                () -> assertNull(search("Hauptstr 34").getMap("name")),
                () -> assertEquals(makeMap("population", "many", "capital", "yes"),
                                   search("München").getMap("extra"))
        );
    }

    @Test
    void testGetLocalized() {
        PhotonResult result = search("München");

        assertAll("getLocalized",
                () -> assertEquals("Munich", result.getLocalised("name", "en")),
                () -> assertEquals("München", result.getLocalised("name", "fr")),
                () -> assertEquals("Monacco", result.getLocalised("name", "it")),
                () -> assertNull(result.getLocalised("city", "en"))
        );
    }

    @Test
    void testGetCoordinates() {
        assertAll("getCoordinates",
                () -> assertArrayEquals(new double[]{-179, 1.0001}, search("Hauptstr 34").getCoordinates()),
                () -> assertArrayEquals(PhotonResult.INVALID_COORDINATES, search("nowhere").getCoordinates())
        );
    }

    @Test
    void testGetExtent() {
        assertAll("getExtent",
                () -> assertNull(search("Munich").getExtent()),
                () -> assertArrayEquals(new double[]{-179.5, 1.1, -178.5, 1.0}, search("hauptstr 34").getExtent())
        );
    }

    @Test
    void testGetScore() {
        assertTrue(Double.isFinite(search("null island").getScore()));
    }
}

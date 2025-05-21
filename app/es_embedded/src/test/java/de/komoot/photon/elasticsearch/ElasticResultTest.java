package de.komoot.photon.elasticsearch;

import de.komoot.photon.*;
import de.komoot.photon.Importer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import de.komoot.photon.query.SimpleSearchRequest;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticResultTest  extends ESBaseTester {
    protected PhotonDoc createDoc(double lon, double lat, int id, int osmId, String key, String value) {
        Point location = FACTORY.createPoint(new Coordinate(lon, lat));
        return new PhotonDoc(id, "W", osmId, key, value).centroid(location);
    }


    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        final var dbProperties = new DatabaseProperties();
        dbProperties.setLanguages(new String[]{"en", "de", "fr", "it"});
        dbProperties.setExtraTags(List.of("population",  "capital"));
        Importer instance = getServer().createImporter(dbProperties);

        instance.add(List.of(
                createDoc(45.2, -7.45, 123, 1123, "place", "city")
                        .names(makeDocNames("name", "München", "name:it", "Monacco", "name:en", "Munich"))
                        .addAddresses(Map.of("state", "Bavaria"), getProperties().getLanguages())
                        .countryCode("de")
                        .extraTags(Map.of("population", "many", "capital", "yes", "maxage", "99"))));
        instance.add(List.of(
                createDoc(0, 0, 99, 11999, "place", "locality")
                        .names(makeDocNames("name", "null island"))));
        instance.add(List.of(
                createDoc(-179, 1.0001, 923, 1923, "place", "house")
                        .houseNumber("34")
                        .bbox(FACTORY.createMultiPointFromCoords(
                                new Coordinate[]{new Coordinate(-179.5, 1.0), new Coordinate(-178.5, 1.1)}))
                        .addAddresses(Map.of("street", "Hauptstr", "city", "Hamburg"),
                                getProperties().getLanguages())));
        instance.add(List.of(
                new PhotonDoc(42, "N", 42, "place", "hamlet")
                        .names(makeDocNames("name", "nowhere"))));

        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        shutdownES();
    }

    private PhotonResult search(String query) {
        var handler = getServer().createSearchHandler(new String[]{"en", "de", "it"}, 1);
        final var request = new SimpleSearchRequest();
        request.setQuery(query);

        return handler.search(request).get(0);
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
                () -> assertEquals(Map.of("default", "München", "en", "Munich", "it", "Monacco"),
                                   search("München").getMap("name")),
                () -> assertEquals(Map.of("default", "null island"),
                                   search("null island").getMap("name")),
                () -> assertNull(search("Hauptstr 34").getMap("name")),
                () -> assertEquals(Map.of("population", "many", "capital", "yes"),
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

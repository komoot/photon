package de.komoot.photon.opensearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

class ImporterTest extends ESBaseTester {

    @BeforeEach
    public void setUp(@TempDir Path dataDirectory) throws IOException {
        getProperties().setSupportGeometries(true);
        setUpES(dataDirectory);
    }

    @Test
    void testAddSimpleDoc() throws ParseException {
        Importer instance = makeImporter();

        instance.add(List.of(
                new PhotonDoc("1234", "N", 1000, "place", "city")
                        .geometry(new WKTReader().read("MULTIPOLYGON (((6.111933 51.2659309, 6.1119417 51.2659247, 6.1119554 51.2659249, 6.1119868 51.2659432, 6.111964 51.2659591, 6.1119333 51.2659391, 6.111933 51.2659309)))"))
                        .extraTags(Collections.singletonMap("maxspeed", "100"))));
        instance.finish();

        PhotonResult response = getById(1234);

        assertNotNull(response);

        assertEquals("N", response.get("osm_type"));
        assertEquals(1000, response.get("osm_id"));
        assertEquals("place", response.get("osm_key"));
        assertEquals("city", response.get("osm_value"));

        assertNull(response.get("extra"));
    }

    @Test
    void testAddHousenumberMultiDoc() {
        Importer instance = makeImporter();

        instance.add(List.of(
                new PhotonDoc("4432", "N", 100, "building", "yes").houseNumber("34"),
                new PhotonDoc("4432", "N", 100, "building", "yes").houseNumber("35")));
        instance.finish();

        PhotonResult response = getById("4432");

        assertNotNull(response);

        assertEquals("N", response.get("osm_type"));
        assertEquals(100, response.get("osm_id"));
        assertEquals("building", response.get("osm_key"));
        assertEquals("yes", response.get("osm_value"));
        assertEquals("34", response.get("housenumber"));

        response = getById("4432.1");

        assertNotNull(response);

        assertEquals("N", response.get("osm_type"));
        assertEquals(100, response.get("osm_id"));
        assertEquals("building", response.get("osm_key"));
        assertEquals("yes", response.get("osm_value"));
        assertEquals("35", response.get("housenumber"));
    }

    @Test
    void testSelectedExtraTagsCanBeIncluded() {
        getProperties().setExtraTags(List.of("maxspeed", "website"));
        Importer instance = makeImporter();

        Map<String, String> extratags = new HashMap<>();
        extratags.put("website", "foo");
        extratags.put("maxspeed", "100 mph");
        extratags.put("source", "survey");

        instance.add(List.of(
                new PhotonDoc("1234", "N", 1000, "place", "city")
                        .extraTags(extratags)));
        instance.add(List.of(
                new PhotonDoc("1235", "N", 1001, "place", "city")
                        .extraTags(Collections.singletonMap("wikidata", "100"))));
        instance.finish();

        PhotonResult response = getById(1234);
        assertNotNull(response);

        Map<String, String> extra = response.getMap("extra");
        assertNotNull(extra);

        assertEquals(2, extra.size());
        assertEquals("100 mph", extra.get("maxspeed"));
        assertEquals("foo", extra.get("website"));

        response = getById(1235);
        assertNotNull(response);

        assertNull(response.get("extra"));
    }

    @Test
    void testUsingPlaceIdsTwice() {
        Importer instance = makeImporter();

        instance.add(List.of(
                new PhotonDoc("113344", "N", 1, "place", "yes")
        ));
        // Photon will bail out on an existing place ID.
        instance.add(List.of(
                new PhotonDoc("113344", "N", 2, "place", "yes")
        ));

        assertThatRuntimeException()
                .isThrownBy(() -> instance.finish())
                .withMessageContaining("Error inserting new documents");
    }

    @Test
    void testImportWithoutPlaceId() {
        Importer instance = makeImporter();

        for (long i = 0; i < 10; ++i) {
            instance.add(List.of(new PhotonDoc().osmType("XXR").osmId(i).houseNumber(Long.toString(i))));
        }
        instance.finish();

        assertThat(getAll())
                .extracting(p -> p.get("osm_id"))
                .containsExactlyInAnyOrder(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }
}
package de.komoot.photon.elasticsearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ImporterTest extends ESBaseTester {

    @BeforeEach
    public void setUp() throws IOException {
        setUpES();
    }

    @Test
    void testAddSimpleDoc() {
        Importer instance = makeImporterWithExtra("");

        instance.add(new PhotonDoc(1234, "N", 1000, "place", "city")
                .extraTags(Collections.singletonMap("maxspeed", "100")), 0);
        instance.finish();
        refresh();

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
        Importer instance = makeImporterWithExtra("");

        instance.add(new PhotonDoc(4432, "N", 100, "building", "yes").houseNumber("34"), 0);
        instance.add(new PhotonDoc(4432, "N", 100, "building", "yes").houseNumber("35"), 1);
        instance.finish();
        refresh();

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
        Importer instance = makeImporterWithExtra("maxspeed", "website");

        Map<String, String> extratags = new HashMap<>();
        extratags.put("website", "foo");
        extratags.put("maxspeed", "100 mph");
        extratags.put("source", "survey");

        instance.add(new PhotonDoc(1234, "N", 1000, "place", "city").extraTags(extratags), 0);
        instance.add(new PhotonDoc(1235, "N", 1001, "place", "city")
                .extraTags(Collections.singletonMap("wikidata", "100")), 0);
        instance.finish();
        refresh();

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
}
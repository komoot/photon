package de.komoot.photon.elasticsearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import org.elasticsearch.action.get.GetResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ImporterTest extends ESBaseTester {

    @Before
    public void setUp() throws IOException {
        setUpES();
    }

    @Test
    public void testAddSimpleDoc() {
        Importer instance = makeImporterWithExtra("");

        instance.add(new PhotonDoc(1234, "N", 1000, "place", "city")
                .extraTags(Collections.singletonMap("maxspeed", "100")));
        instance.finish();
        refresh();

        GetResponse response = getById(1234);

        assertTrue(response.isExists());

        Map<String, Object> source = response.getSource();

        assertEquals("N", source.get("osm_type"));
        assertEquals(1000, source.get("osm_id"));
        assertEquals("place", source.get("osm_key"));
        assertEquals("city", source.get("osm_value"));

        assertNull(source.get("extra"));
    }

    @Test
    public void testSelectedExtraTagsCanBeIncluded() {
        Importer instance = makeImporterWithExtra("maxspeed,website");

        Map<String, String> extratags = new HashMap<>();
        extratags.put("website", "foo");
        extratags.put("maxspeed", "100 mph");
        extratags.put("source", "survey");

        instance.add(new PhotonDoc(1234, "N", 1000, "place", "city").extraTags(extratags));
        instance.add(new PhotonDoc(1235, "N", 1001, "place", "city")
                .extraTags(Collections.singletonMap("wikidata", "100")));
        instance.finish();
        refresh();

        GetResponse response = getById(1234);
        assertTrue(response.isExists());

        Map<String, String> extra = (Map<String, String>) response.getSource().get("extra");
        assertNotNull(extra);

        assertEquals(2, extra.size());
        assertEquals("100 mph", extra.get("maxspeed"));
        assertEquals("foo", extra.get("website"));

        response = getById(1235);
        assertTrue(response.isExists());

        assertNull(response.getSource().get("extra"));
    }
}
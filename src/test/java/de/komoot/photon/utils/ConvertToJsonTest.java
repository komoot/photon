package de.komoot.photon.utils;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Importer;
import de.komoot.photon.query.PhotonRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConvertToJsonTest extends ESBaseTester {

    private List<JSONObject> databaseFromDoc(PhotonDoc doc) throws IOException {
        final String name = "abcde";
        doc.names(Collections.singletonMap("name", name));

        setUpES();
        Importer instance = makeImporterWithExtra("maxspeed","website");
        instance.add(doc);
        instance.finish();
        refresh();

        // Indirectly calls converttojson
        return getServer().createSearchHandler(new String[]{"en", "de", "fr"}).search(new PhotonRequest(name));
    }

    @Test
    public void testConvertWithExtraTags() throws IOException {
        Map<String, String> extratags = new HashMap<>();
        extratags.put("website", "foo");
        extratags.put("maxspeed", "100 mph");

        List<JSONObject> json = databaseFromDoc(new PhotonDoc(1234, "N", 1000, "place", "city").extraTags(extratags));

        JSONObject extra = json.get(0).getJSONObject("properties").getJSONObject("extra");

        assertEquals(2, extra.length());
        assertEquals("foo", extra.getString("website"));
        assertEquals("100 mph", extra.getString("maxspeed"));
    }


    @Test
    public void testConvertWithoutExtraTags() throws IOException {
        List<JSONObject> json = databaseFromDoc(new PhotonDoc(1234, "N", 1000, "place", "city"));

        assertNull(json.get(0).getJSONObject("properties").optJSONObject("extra"));
    }
}

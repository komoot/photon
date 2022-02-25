package de.komoot.photon.query;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PhotonRequestTest {

    private PhotonRequest simpleRequest() {
        return new PhotonRequest("foo", 1, null, null, 1.6, 16, "de", false);
    }

    @Test
    public void testIncludePlaces() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{"key:value", "12!:34"});

        Map<String, Set<String>> tags = request.tags();
        assertEquals(2, tags.size());
        assertEquals(new HashSet<>(Arrays.asList("value")), tags.get("key"));
        assertEquals(new HashSet<>(Arrays.asList("34")), tags.get("12!"));
    }

    @Test
    public void testExcludePlaces() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{"!foo:234", "!foo:!abc"});

        Map<String, Set<String>> tags = request.notTags();
        assertEquals(1, tags.size());
        assertEquals(new HashSet<>(Arrays.asList("234", "abc")), tags.get("foo"));
    }

    @Test
    public void testIncludeKey() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{"highway", "path"});

        assertEquals(new HashSet<>(Arrays.asList("highway", "path")), request.keys());
    }

    @Test
    public void testExcludeKey() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{"!amenity", "!place"});

        assertEquals(new HashSet<>(Arrays.asList("amenity", "place")), request.notKeys());
    }


    @Test
    public void testIncludeValue() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{":hotel", ":restaurant"});

        assertEquals(new HashSet<>(Arrays.asList("hotel", "restaurant")), request.values());
    }

    @Test
    public void testExcludeValue() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{":!123", ":!1234"});

        assertEquals(new HashSet<>(Arrays.asList("123", "1234")), request.notValues());
    }

    @Test
    public void testExcludeTagValues() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{"foo:!bar"});

        Map<String, Set<String>> tags = request.tagNotValues();
        assertEquals(1, tags.size());
        assertEquals(new HashSet<>(Arrays.asList("bar")), tags.get("foo"));
    }
}

package de.komoot.photon.query;

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals(2, tags.size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("value")), tags.get("key"));
        Assert.assertEquals(new HashSet<>(Arrays.asList("34")), tags.get("12!"));
    }

    @Test
    public void testExcludePlaces() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{"!foo:234", "!foo:!abc"});

        Map<String, Set<String>> tags = request.notTags();
        Assert.assertEquals(1, tags.size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("234", "abc")), tags.get("foo"));
    }

    @Test
    public void testIncludeKey() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{"highway", "path"});

        Assert.assertEquals(new HashSet<>(Arrays.asList("highway", "path")), request.keys());
    }

    @Test
    public void testExcludeKey() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{"!amenity", "!place"});

        Assert.assertEquals(new HashSet<>(Arrays.asList("amenity", "place")), request.notKeys());
    }


    @Test
    public void testIncludeValue() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{":hotel", ":restaurant"});

        Assert.assertEquals(new HashSet<>(Arrays.asList("hotel", "restaurant")), request.values());
    }

    @Test
    public void testExcludeValue() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{":!123", ":!1234"});

        Assert.assertEquals(new HashSet<>(Arrays.asList("123", "1234")), request.notValues());
    }

    @Test
    public void testExcludeTagValues() {
        PhotonRequest request = simpleRequest();
        request.setUpTagFilters(new String[]{"foo:!bar"});

        Map<String, Set<String>> tags = request.tagNotValues();
        Assert.assertEquals(1, tags.size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("bar")), tags.get("foo"));
    }
}

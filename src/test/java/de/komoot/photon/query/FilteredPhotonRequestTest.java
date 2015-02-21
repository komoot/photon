package de.komoot.photon.query;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.komoot.photon.ReflectionTestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FilteredPhotonRequestTest {

    @Test
    public void testNotKey() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null, null, null,null);
        filteredPhotonRequest.notKey("exclude");
        filteredPhotonRequest.notKey("exclude");
        filteredPhotonRequest.notKey("anotherExclude");
        Set<String> excludeKeys = ReflectionTestUtil.getFieldValue(filteredPhotonRequest, FilteredPhotonRequest.class, "excludeKeys");
        Iterator<String> iterator = excludeKeys.iterator();
        Assert.assertEquals("anotherExclude", iterator.next());
        Assert.assertEquals("exclude", iterator.next());
        Assert.assertEquals(2, excludeKeys.size());
    }

    @Test
    public void testNotTag() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null, null, null,null);
        filteredPhotonRequest.notTag("aKey", "aValue");
        filteredPhotonRequest.notTag("anotherKey", "anotherValue");
        Map<String, String> excludeTags = filteredPhotonRequest.notTag();
        Assert.assertEquals(ImmutableMap.of("anotherKey","anotherValue", "aKey","aValue"), excludeTags);
        Assert.assertEquals(2, excludeTags.size());
    }

    @Test
    public void testNotValue() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null, null, null,null);
        filteredPhotonRequest.notValue("exclude");
        filteredPhotonRequest.notValue("exclude");
        filteredPhotonRequest.notValue("anotherExclude");
        Set<String> excludeValues = filteredPhotonRequest.notValue();
        Assert.assertEquals(ImmutableSet.of("anotherExclude","exclude"), excludeValues);
        Assert.assertEquals(2, excludeValues.size());
    }

    @Test
    public void testKey() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null, null, null,null);
        filteredPhotonRequest.key("keyToInclude");
        filteredPhotonRequest.key("keyToInclude");
        filteredPhotonRequest.key("anotherKeyToInclude");
        Assert.assertEquals(ImmutableSet.of("keyToInclude","anotherKeyToInclude"),filteredPhotonRequest.key());
    }

    @Test
    public void testTag() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null,null, null, null);
        filteredPhotonRequest.tag("aKey", "aValue");
        filteredPhotonRequest.tag("anotherKey", "anotherValue");
        Map<String, String> includeTags = filteredPhotonRequest.tag();
        Assert.assertEquals(ImmutableMap.of("anotherKey","anotherValue", "aKey","aValue"), includeTags);
        Assert.assertEquals(2, includeTags.size());
        
    }

    @Test
    public void testValue() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null,null, null, null);
        filteredPhotonRequest.value("keyToInclude");
        filteredPhotonRequest.value("keyToInclude");
        filteredPhotonRequest.value("anotherKeyToInclude");
        Assert.assertEquals(ImmutableSet.of("keyToInclude","anotherKeyToInclude"),filteredPhotonRequest.value());
    }
}
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
        filteredPhotonRequest.notKeys("exclude");
        filteredPhotonRequest.notKeys("exclude");
        filteredPhotonRequest.notKeys("anotherExclude");
        Set<String> excludeKeys = ReflectionTestUtil.getFieldValue(filteredPhotonRequest, FilteredPhotonRequest.class, "excludeKeys");
        Iterator<String> iterator = excludeKeys.iterator();
        Assert.assertEquals("anotherExclude", iterator.next());
        Assert.assertEquals("exclude", iterator.next());
        Assert.assertEquals(2, excludeKeys.size());
    }

    @Test
    public void testNotTag() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null, null, null,null);
        filteredPhotonRequest.notTags("aKey", ImmutableSet.of("aValue"));
        filteredPhotonRequest.notTags("anotherKey", ImmutableSet.of("anotherValue"));
        Map<String, Set<String>> excludeTags = filteredPhotonRequest.notTags();
        Assert.assertEquals(ImmutableMap.of("anotherKey",ImmutableSet.of("anotherValue"), "aKey",ImmutableSet.of("aValue")), excludeTags);
        Assert.assertEquals(2, excludeTags.size());
    }

    @Test
    public void testNotValue() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null, null, null,null);
        filteredPhotonRequest.notValues("exclude");
        filteredPhotonRequest.notValues("exclude");
        filteredPhotonRequest.notValues("anotherExclude");
        Set<String> excludeValues = filteredPhotonRequest.notValues();
        Assert.assertEquals(ImmutableSet.of("anotherExclude","exclude"), excludeValues);
        Assert.assertEquals(2, excludeValues.size());
    }

    @Test
    public void testKey() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null, null, null,null);
        filteredPhotonRequest.keys("keyToInclude");
        filteredPhotonRequest.keys("keyToInclude");
        filteredPhotonRequest.keys("anotherKeyToInclude");
        Assert.assertEquals(ImmutableSet.of("keyToInclude","anotherKeyToInclude"),filteredPhotonRequest.keys());
    }

    @Test
    public void testTag() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null,null, null, null);
        filteredPhotonRequest.tags("aKey", ImmutableSet.of("aValue"));
        filteredPhotonRequest.tags("anotherKey", ImmutableSet.of("anotherValue"));
        Map<String, Set<String>> includeTags = filteredPhotonRequest.tags();
        Assert.assertEquals(ImmutableMap.of("anotherKey",ImmutableSet.of("anotherValue"), "aKey",ImmutableSet.of("aValue")), includeTags);
        Assert.assertEquals(2, includeTags.size());
        
    }

    @Test
    public void testValue() {
        FilteredPhotonRequest filteredPhotonRequest = new FilteredPhotonRequest(null,null, null, null);
        filteredPhotonRequest.values("keyToInclude");
        filteredPhotonRequest.values("keyToInclude");
        filteredPhotonRequest.values("anotherKeyToInclude");
        Assert.assertEquals(ImmutableSet.of("keyToInclude","anotherKeyToInclude"),filteredPhotonRequest.values());
    }
}
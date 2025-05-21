package de.komoot.photon.nominatim.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContextMapTest {
        private ContextMap testMap() {
        ContextMap map = new ContextMap();
        map.addName("default", "n1");
        map.addName("default", "n2");
        map.addName("old", "former");
        return map;
    }

    @Test
    void testAddName() {
        ContextMap map = new ContextMap();

        assertTrue(map.isEmpty());

        map.addName("default", "something");
        assertEquals(Set.of("something"), map.get("default"));

        map.addName("alt", "else");
        assertEquals(Set.of("something"), map.get("default"));
        assertEquals(Set.of("else"), map.get("alt"));

        map.addName("default", "45");
        assertEquals(Set.of("something", "45"), map.get("default"));
        assertEquals(Set.of("else"), map.get("alt"));

        map.addName("alt", "else");
        assertEquals(Set.of("something", "45"), map.get("default"));
        assertEquals(Set.of("else"), map.get("alt"));
    }

    @Test
    void testAddFromSimpleMap() {
        ContextMap map = testMap();

        map.addAll(Map.of("alt", "XX", "default", "n3", "old", "former"));

        assertEquals(3, map.size());
        assertEquals(Set.of("n1", "n2", "n3"), map.get("default"));
        assertEquals(Set.of("XX"), map.get("alt"));
        assertEquals(Set.of("former"), map.get("old"));
    }

    @Test
    void testAddFromContextMap() {
        ContextMap map = testMap();

        ContextMap other = new ContextMap();
        other.addName("default", "n1");
        other.addName("default", "n3");
        other.addName("alt", "XX");
        other.addName("alt", "YY");

        map.addAll(other);
        assertEquals(3, map.size());
        assertEquals(Set.of("n1", "n2", "n3"), map.get("default"));
        assertEquals(Set.of("XX", "YY"), map.get("alt"));
        assertEquals(Set.of("former"), map.get("old"));

        map.addName("alt", "ZZ");
        assertEquals(Set.of("XX", "YY", "ZZ"), map.get("alt"));
        assertEquals(Set.of("XX", "YY"), other.get("alt"));
    }
}

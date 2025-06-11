package de.komoot.photon.searcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TagFilterTest {

    @ParameterizedTest
    @MethodSource("validOsmTagFilterValueProvider")
    void testBuildOsmTagFilterOk(String filter, TagFilterKind kind, String key, String value) {
        assertEquals(new TagFilter(kind, key, value),
                TagFilter.buildOsmTagFilter(filter));
    }

    static Stream<Arguments> validOsmTagFilterValueProvider() {
        return Stream.of(
                arguments("tourism", TagFilterKind.INCLUDE, "tourism", null),
                arguments(":information", TagFilterKind.INCLUDE, null, "information"),
                arguments("shop:bakery", TagFilterKind.INCLUDE, "shop", "bakery"),
                arguments("!highway", TagFilterKind.EXCLUDE, "highway", null),
                arguments("!:path", TagFilterKind.EXCLUDE, null, "path"),
                arguments(":!path", TagFilterKind.EXCLUDE, null, "path"),
                arguments("!highway:path", TagFilterKind.EXCLUDE, "highway", "path"),
                arguments("!highway:!path", TagFilterKind.EXCLUDE, "highway", "path"),
                arguments("amenity:!post_box", TagFilterKind.EXCLUDE_VALUE, "amenity", "post_box"),
                arguments("extra.transport_modes:onstreetTram", TagFilterKind.INCLUDE, "extra.transport_modes", "onstreetTram")
        );
    }


    @ParameterizedTest
    @ValueSource(strings = {"", ":", "addr:housenumber:1", "shop:"})
    void testBuildOsmTagFilterInvalid(String filter) {
        assertNull(TagFilter.buildOsmTagFilter(filter));
    }

}
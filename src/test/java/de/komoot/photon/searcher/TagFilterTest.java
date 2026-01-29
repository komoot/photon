package de.komoot.photon.searcher;

import de.komoot.photon.query.BadRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import static org.assertj.core.api.Assertions.*;

class TagFilterTest {

    @ParameterizedTest
    @MethodSource("validOsmTagFilterValueProvider")
    void testBuildOsmTagFilterOk(String filter, TagFilterKind kind, String key, String value) {
        assertThat(TagFilter.buildOsmTagFilter(filter))
                .hasFieldOrPropertyWithValue("kind", kind)
                .hasFieldOrPropertyWithValue("key", key)
                .hasFieldOrPropertyWithValue("value", value)
        ;
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
                arguments("amenity:!post_box", TagFilterKind.EXCLUDE_VALUE, "amenity", "post_box")
        );
    }


    @ParameterizedTest
    @ValueSource(strings = {"", ":", "addr:housenumber:1", "shop:"})
    void testBuildOsmTagFilterInvalid(String filter) {
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> TagFilter.buildOsmTagFilter(filter));
    }

}
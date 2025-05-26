package de.komoot.photon.nominatim.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class NameMapTest {

    @ParameterizedTest
    @MethodSource("validPlaceNameProvider")
    void testPlaceName(Map<String, String> input, Map<String, String> output) {
        assertThat(NameMap.makeForPlace(input, new String[]{"en", "it"}))
                .isEqualTo(output);
    }

    static Stream<Arguments> validPlaceNameProvider() {
        return Stream.of(
                arguments(
                        Map.of("name", "ABC"),
                        Map.of("default", "ABC")),
                arguments(
                        Map.of("name", "ABC", "_place_name", "CBA"),
                        Map.of("default", "CBA")),
                arguments(
                        Map.of("name", "standard", "name:en", "foo", "name:de:it", "fooi", "name:ch", "f"),
                        Map.of("default", "standard", "en", "foo")),
                arguments(
                        Map.of("name", "this", "alt_name", "that", "old_name", "dis"),
                        Map.of("default", "this", "alt", "that", "old", "dis"))
        );
    }

}

package de.komoot.photon.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class StringArrayConverterTest {

    @ParameterizedTest
    @MethodSource("convertArgumentProvider")
    void testConvert(String arg, String[] expected) {
        assertArrayEquals(expected, new StringArrayConverter().convert(arg));
    }

    static Stream<Arguments> convertArgumentProvider() {
        return Stream.of(
                arguments("foo", new String[]{"foo"}),
                arguments("with space", new String[]{"with space"}),
                arguments(" 123", new String[]{"123"}),
                arguments("", new String[]{}),
                arguments(",", new String[]{}),
                arguments("1,2,3", new String[]{"1", "2", "3"}),
                arguments("1, 2, 3", new String[]{"1", "2", "3"}),
                arguments("a,,b", new String[]{"a", "b"}),
                arguments("a, ,b", new String[]{"a", "b"})
        );
    }
}
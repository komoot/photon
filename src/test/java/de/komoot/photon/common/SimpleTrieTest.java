package de.komoot.photon.common;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class SimpleTrieTest {

    <T> AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertLongestPrefix(SimpleTrie<T> trie, String word, int start) {
        return assertThat(trie.longestPrefix(word, start))
                .extracting("value", "endpos");
    }

    static private Stream<String> permutate(String... input) {
        return Stream.of(Arrays.stream(input).collect(Collectors.toList())).mapMulti((inputs, cons) -> {
            var len = inputs.size();
            var indexes = new int[len];

            cons.accept(String.join(",", inputs));
            int i = 0;
            while (i < len) {
                if (indexes[i] < i) {
                    Collections.swap(inputs, i % 2 == 0 ?  0: indexes[i], i);
                    cons.accept(String.join(",", inputs));
                    ++indexes[i];
                    i = 0;
                } else {
                    indexes[i] = 0;
                    ++i;
                }
            }
        });
    }

    private SimpleTrie<String> mkTrie(String listInput) {
        var trie = new SimpleTrie<String>();
        var entries = new ArrayList<SimpleTrie.Entry<String>>();
        for (var s : listInput.split(",")) {
            trie.put(s, s);
            entries.add(new SimpleTrie.Entry<>(s, s));
        }

        assertThat(trie).containsExactlyInAnyOrder(entries.toArray(new SimpleTrie.Entry[]{}));

        return trie;
    }

    @Test
    void testEmptyTrie() {
        var t = new SimpleTrie<Integer>();

        assertThat(t.isEmpty()).isTrue();
        assertThat(t.longestPrefix("", 0)).isNull();
        assertThat(t.longestPrefix("abc", 0)).isNull();
        assertThat(t.longestPrefix("abc", 1)).isNull();
        assertThat(t.longestPrefix("abc", 5)).isNull();

        assertThat(t).containsExactly();
    }

    @Test
    void testSingleEntryTrie() {
        var t = new SimpleTrie<Integer>();
        t.put("abc", 100);

        assertThat(t.isEmpty()).isFalse();
        assertThat(t.longestPrefix("", 0)).isNull();
        assertThat(t.longestPrefix("ab", 0)).isNull();
        assertLongestPrefix(t,"abc", 0).containsExactly(100, 3);
        assertLongestPrefix(t,"abcde", 0).containsExactly(100, 3);
        assertLongestPrefix(t,"aabcd", 1).containsExactly(100, 4);

        assertThat(t).containsExactlyInAnyOrder(
                new SimpleTrie.Entry<>("abc", 100)
        );
    }

    static Stream<String> testDistinctWordsTrie() { return permutate("ab", "123"); }

    @ParameterizedTest
    @MethodSource
    void testDistinctWordsTrie(String listInput) {
        var t = mkTrie(listInput);

        assertThat(t.isEmpty()).isFalse();
        assertLongestPrefix(t,"abba", 0).containsExactly("ab", 2);
        assertLongestPrefix(t,"123", 0).containsExactly("123", 3);
        assertLongestPrefix(t,"ab123", 2).containsExactly("123", 5);
        assertThat(t.longestPrefix("b", 0)).isNull();
        assertThat(t.longestPrefix("ab123", 1)).isNull();
    }

    static Stream<String> testSamePrefixTrie() { return permutate("house", "hord"); }

    @ParameterizedTest
    @MethodSource
    void testSamePrefixTrie(String listInput) {
        var t = mkTrie(listInput);

        assertThat(t.isEmpty()).isFalse();
        assertLongestPrefix(t,"house", 0).containsExactly("house", 5);
        assertLongestPrefix(t,"hord", 0).containsExactly("hord", 4);
        assertLongestPrefix(t,"horder", 0).containsExactly("hord", 4);
        assertThat(t.longestPrefix("ho", 0)).isNull();
    }

    static Stream<String> testContainedWordsTrie() { return permutate("pen", "pens", "penta"); }

    @ParameterizedTest
    @MethodSource
    void testContainedWordsTrie(String listInput) {
        var t = mkTrie(listInput);

        assertThat(t.isEmpty()).isFalse();
        assertLongestPrefix(t,"pen", 0).containsExactly("pen", 3);
        assertLongestPrefix(t,"pens", 0).containsExactly("pens", 4);
        assertLongestPrefix(t,"penta", 0).containsExactly("penta", 5);
        assertLongestPrefix(t,"penship", 0).containsExactly("pens", 4);
        assertLongestPrefix(t,"penn", 0).containsExactly("pen", 3);
        assertThat(t.longestPrefix("pe", 0)).isNull();
    }
}
package de.komoot.photon.analyzer.config;

import de.komoot.photon.UsageException;
import de.komoot.photon.analyzer.normalization.ICUNormalizer;
import de.komoot.photon.analyzer.normalization.NullNormalizer;
import de.komoot.photon.common.SimpleTrie;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class LexicalRulesConfigTest {

    LexicalRulesConfig configFromRules(String... rules) {
        var content = new StringBuilder("words:\n");
        for (var rule : rules) {
            content.append("    - ").append(rule).append("\n");
        }

        return YAMLMapper.shared().readValue(content.toString(), LexicalRulesConfig.class);
    }

    private static AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertThatReplacementTree(LexicalRulesConfig cfg) {
        return assertThat(cfg.getReplacementTree(NullNormalizer.shared()))
                .extracting(SimpleTrie.Entry::key,
                        e -> new HashSet<>(e.value()));
    }

    private static AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertThatReplacementTree(LexicalRulesConfig cfg, List<String> normalizerRules) {
        return assertThat(cfg.getReplacementTree(new ICUNormalizer("test", normalizerRules)))
                .extracting(SimpleTrie.Entry::key,
                        e -> new HashSet<>(e.value()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo > bar", "foo -> bar -> bar", "~foo~ -> bar", "fo~ o -> bar"})
    void testIllegalRules(String rule) {
        var cfg = configFromRules(rule);
        assertThatExceptionOfType(UsageException.class)
                .isThrownBy(() -> cfg.getReplacementTree(NullNormalizer.shared()));
    }

    @Test
    void testAddFull() {
        assertThatReplacementTree(configFromRules("foo -> bar", "Community Hall -> CH"))
                .containsExactlyInAnyOrder(
                        tuple(" foo ", Set.of(" bar", " foo")),
                        tuple(" Community Hall ", Set.of(" Community Hall", " CH"))
                );
    }

    @Test
    void testReplaceFull() {
        assertThatReplacementTree(configFromRules("foo => bar"))
                .containsExactlyInAnyOrder(
                        tuple(" foo ", Set.of(" bar"))
                );
    }

    @Test
    void testAddSuffixNoDecompose() {
        assertThatReplacementTree(configFromRules("~berg |-> bg"))
                .containsExactlyInAnyOrder(
                        tuple(" berg ", Set.of(" berg", " bg")),
                        tuple("berg ", Set.of("berg", "bg"))
                );
    }

    @Test
    void testReplaceSuffixNoDecompose() {
        assertThatReplacementTree(configFromRules("~berg |=> bg"))
                .containsExactlyInAnyOrder(
                        tuple(" berg ", Set.of(" bg")),
                        tuple("berg ", Set.of("bg"))
                );
    }

    @Test
    void testAddSuffixDecompose() {
        assertThatReplacementTree(configFromRules("~berg -> bg"))
                .containsExactlyInAnyOrder(
                        tuple(" berg ", Set.of(" berg", " bg", "berg", "bg")),
                        tuple("berg ", Set.of(" berg", " bg", "berg", "bg"))
                );
    }

    @Test
    void testReplaceSuffixDecompose() {
        assertThatReplacementTree(configFromRules("~berg => bg"))
                .containsExactlyInAnyOrder(
                        tuple(" berg ", Set.of("bg", " bg")),
                        tuple("berg ", Set.of("bg", " bg"))
                );
    }

    @Test
    void testAddPrefixNoDecompose() {
        assertThatReplacementTree(configFromRules("hinter~ |-> hnt"))
                .containsExactlyInAnyOrder(
                        tuple(" hinter", Set.of(" hinter", " hnt")),
                        tuple(" hinter ", Set.of(" hinter", " hnt"))
                );
    }

    @Test
    void testReplacePrefixNoDecompose() {
        assertThatReplacementTree(configFromRules("hinter~ |=> hnt"))
                .containsExactlyInAnyOrder(
                        tuple(" hinter", Set.of(" hnt")),
                        tuple(" hinter ", Set.of(" hnt"))
                );
    }

    @Test
    void testAddPrefixDecompose() {
        assertThatReplacementTree(configFromRules("hinter~ -> hnt"))
                .containsExactlyInAnyOrder(
                        tuple(" hinter", Set.of(" hinter", " hinter ", " hnt", " hnt ")),
                        tuple(" hinter ", Set.of(" hinter", " hnt"))
                );
    }

    @Test
    void testReplacePrefixDecompose() {
        assertThatReplacementTree(configFromRules("hinter~ => hnt"))
                .containsExactlyInAnyOrder(
                        tuple(" hinter", Set.of(" hnt", " hnt ")),
                        tuple(" hinter ", Set.of(" hnt"))
                );
    }

    @Test
    void testAddBeginningOnly() {
        assertThatReplacementTree(configFromRules("^Premier -> Pr"))
                .containsExactlyInAnyOrder(
                        tuple("^ Premier ", Set.of("^ Pr", "^ Premier"))
                );
    }

    @Test
    void testReplaceBeginningOnly() {
        assertThatReplacementTree(configFromRules("^Premier => Pr"))
                .containsExactlyInAnyOrder(
                        tuple("^ Premier ", Set.of("^ Pr"))
                );
    }

    @Test
    void testAddFinalOnly() {
        assertThatReplacementTree(configFromRules("road$ -> rd"))
                .containsExactlyInAnyOrder(
                        tuple(" road ^", Set.of(" rd ^", " road ^"))
                );
    }

    @Test
    void testReplaceFinalOnly() {
        assertThatReplacementTree(configFromRules("road$ => rd"))
                .containsExactlyInAnyOrder(
                        tuple(" road ^", Set.of(" rd ^"))
                );
    }

    @Test
    void testDecomposeOnly() {
        assertThatReplacementTree(configFromRules("~foo -> foo"))
                .containsExactlyInAnyOrder(
                        tuple(" foo ", Set.of(" foo", "foo")),
                        tuple("foo ", Set.of(" foo", "foo"))
                );
    }

    @Test
    void testAddSuffixDecomposeEndOnly() {
        assertThatReplacementTree(configFromRules("~berg |-> bg", "~berg$ -> bg"))
                .containsExactlyInAnyOrder(
                        tuple(" berg ", Set.of(" berg", " bg")),
                        tuple(" berg ^", Set.of(" berg ^", " bg ^", "berg ^", "bg ^")),
                        tuple("berg ", Set.of("berg", "bg")),
                        tuple("berg ^", Set.of(" berg ^", " bg ^", "berg ^", "bg ^"))
                );
    }

    @Test
    void testReplaceSuffixDecomposeEndOnly() {
        assertThatReplacementTree(configFromRules("~berg |=> bg", "~berg$ => bg"))
                .containsExactlyInAnyOrder(
                        tuple(" berg ", Set.of(" bg")),
                        tuple(" berg ^", Set.of(" bg ^", "bg ^")),
                        tuple("berg ", Set.of("bg")),
                        tuple("berg ^", Set.of(" bg ^", "bg ^"))
                );
    }

    @Test
    void testAddMultipleSuffix() {
        assertThatReplacementTree(configFromRules("~berg,~burg -> bg"))
                .containsExactlyInAnyOrder(
                        tuple(" berg ", Set.of(" berg", "berg", " bg", "bg")),
                        tuple("berg ", Set.of(" berg", "berg", " bg", "bg")),
                        tuple(" burg ", Set.of(" burg", "burg", " bg", "bg")),
                        tuple("burg ", Set.of(" burg", "burg", " bg", "bg"))
                );
    }

    @Test
    void testVariantWithNormalization() {
        assertThatReplacementTree(configFromRules("Road -> Rd"), List.of(":: lower"))
                .containsExactlyInAnyOrder(
                        tuple(" road ", Set.of(" road", " rd"))
                );
    }

    @Test
    void testVariantWithNormalizationPreserveTrailingChars() {
        assertThatReplacementTree(configFromRules("Road -> Rd-"), List.of(" '-' > ' '"))
                .containsExactlyInAnyOrder(
                        tuple(" Road ", Set.of(" Road", " Rd "))
                );
    }}
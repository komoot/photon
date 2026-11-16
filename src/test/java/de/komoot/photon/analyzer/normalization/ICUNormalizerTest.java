package de.komoot.photon.analyzer.normalization;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ICUNormalizerTest {

    @Test
    void testNoRules() {
        assertThat(Normalizer.fromICURules("empty", List.of())).isInstanceOf(NullNormalizer.class);
    }

    @Test
    void testSimpleRule() {
        var norm = Normalizer.fromICURules("test_lower", List.of(":: lower"));

        assertThat(norm.transform("AbC")).isEqualTo("abc");
    }

    @Test
    void testCombinedRule() {
        var norm = Normalizer.fromICURules("test_combined", List.of("a > '@'", "'@' > '!'"));

        assertThat(norm.transform("nonce")).isEqualTo("nonce");
        assertThat(norm.transform("@go")).isEqualTo("!go");
        assertThat(norm.transform("har@d")).isEqualTo("h!r!d");
    }
}
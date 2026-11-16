package de.komoot.photon.analyzer.normalization;

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface Normalizer {

    String transform(String input);

    static Normalizer fromICURules(String id, @Nullable List<String> rules) {
        if (rules == null || rules.isEmpty()) {
            return NullNormalizer.shared();
        }

        return new ICUNormalizer(id, rules);
    }
}

package de.komoot.photon.analyzer.variants;

import de.komoot.photon.analyzer.normalization.ICUNormalizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IcuVariantsGenerator implements VariantGenerator {
    private static int ruleId = 1;

    private final ICUNormalizer normalizer;

    public IcuVariantsGenerator(List<String> rules) {
        normalizer = new ICUNormalizer("photon-word-transformer-" + ruleId++, rules);
    }

    @Override
    public Set<String> transform(Set<String> input) {
        var out = new HashSet<String>();
        for (var word : input) {
            out.add(word);
            out.add(normalizer.transform(word));
        }
        return out;
    }
}

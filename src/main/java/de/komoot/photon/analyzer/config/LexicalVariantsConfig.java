package de.komoot.photon.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.komoot.photon.analyzer.config.nested.NestableArray;
import de.komoot.photon.analyzer.normalization.Normalizer;
import de.komoot.photon.analyzer.variants.LexicalVariantsGenerator;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public class LexicalVariantsConfig implements VariantConfig {

    @JsonProperty
    private final NestableArray<LexicalRulesConfig> rules = new NestableArray<>();

    @Override
    public List<FilterableGenerator> getGenerators(Normalizer normalizer) {
        List<FilterableGenerator> list = new ArrayList<>();
        for (LexicalRulesConfig rule : rules) {
            var t = rule.getReplacementTree(normalizer);
            if (!t.isEmpty()) {
                var transformerConfig = new FilterableGenerator(new LexicalVariantsGenerator(t), rule);
                list.add(transformerConfig);
            }
        }
        return list;
    }
}

package de.komoot.photon.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.komoot.photon.analyzer.config.nested.NestableArray;
import de.komoot.photon.analyzer.normalization.Normalizer;
import de.komoot.photon.analyzer.variants.IcuVariantsGenerator;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public class IcuTransformVariantsConfig implements VariantConfig {

    @JsonProperty
    private final NestableArray<IcuTransformRulesConfig> rules = new NestableArray<>();

    @Override
    public List<FilterableGenerator> getGenerators(Normalizer normalizer) {
        var out = new ArrayList<FilterableGenerator>();

        for (var rule : rules) {
            if (!rule.getRules().isEmpty()) {
                out.add(new FilterableGenerator(new IcuVariantsGenerator(rule.getRules()), rule));
            }
        }

        return out;
    }
}

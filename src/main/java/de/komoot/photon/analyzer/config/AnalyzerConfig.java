package de.komoot.photon.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.komoot.photon.analyzer.config.nested.NestableArray;
import de.komoot.photon.analyzer.config.nested.NestableValue;
import de.komoot.photon.analyzer.config.nested.NestedConfigModule;
import de.komoot.photon.analyzer.filter.FilterCollector;
import de.komoot.photon.analyzer.normalization.ICUNormalizer;
import de.komoot.photon.analyzer.normalization.NullNormalizer;
import de.komoot.photon.analyzer.variants.VariantProcessor;
import org.jspecify.annotations.NullMarked;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.util.*;

@NullMarked
public class AnalyzerConfig {
    @JsonProperty("normalization")
    private Map<String, List<String>> normalizationRules = Map.of();

    @JsonProperty("language-defaults-per-country")
    private NestableValue<Map<String, String>> countryLanguageMap = new NestableValue<>(Map.of());

    @JsonProperty("variants")
    private NestableArray<VariantConfig> processors = new NestableArray<>();

    public List<String> getInputNormalizerRules() {
        return normalizationRules.getOrDefault("input", List.of());
    }

    public VariantProcessor createProcessor(Set<String> languages, List<String> normalizerRules) {
        var normalizer = normalizerRules.isEmpty()? NullNormalizer.shared() : new ICUNormalizer("PhotonInputNormalizer", normalizerRules);
        var transformers = new ArrayList<VariantConfig.FilterableGenerator>();
        var filterLanguages = new HashSet<>(languages);
        filterLanguages.addAll(countryLanguageMap.value().values());

        var filterCollector = new FilterCollector();
        for (var proc : processors) {
            for (var trans : proc.getGenerators(normalizer)) {
                if (trans.filters().lang == null || filterLanguages.contains(trans.filters().lang)) {
                    filterCollector.update(trans.filters());
                    transformers.add(trans);
                }
            }
        }

        var proc = new VariantProcessor(filterLanguages, countryLanguageMap.value(),
                normalizer,
                filterCollector.createPhotonDocFilters(),
                filterCollector.createNameFilters());

        for (var t : transformers) {
            proc.addGenerator(filterCollector.getTransformerFilter(t.filters()), t.generator());
        }

        return proc;
    }


    public static AnalyzerConfig createFromFile(File variantsConfigFile) {
        var module = new NestedConfigModule(variantsConfigFile.getAbsoluteFile().getParentFile().toPath());
        var mapper = YAMLMapper.builder().addModule(module).build();

        return mapper.readValue(variantsConfigFile, AnalyzerConfig.class);
    }

    public static AnalyzerConfig createDefault() {
        var module = new NestedConfigModule(null);
        YAMLMapper mapper = YAMLMapper.builder().addModule(module).build();

        return mapper.readValue(
                AnalyzerConfig.class.getResourceAsStream("/analyzer.yaml"),
                AnalyzerConfig.class);
    }
}
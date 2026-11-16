package de.komoot.photon.analyzer.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.komoot.photon.analyzer.normalization.Normalizer;
import de.komoot.photon.analyzer.variants.VariantGenerator;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LexicalVariantsConfig.class, name = "lexical"),
        @JsonSubTypes.Type(value = IcuTransformVariantsConfig.class, name = "icu-transform")
})
public interface VariantConfig {
    record FilterableGenerator(VariantGenerator generator, FilterConfig filters) {}

    List<FilterableGenerator> getGenerators(Normalizer normalizer);
}

package de.komoot.photon.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public class IcuTransformRulesConfig extends FilterConfig {

    @JsonProperty("transforms")
    private List<String> rules = List.of();

    public List<String> getRules() {
        return rules;
    }
}

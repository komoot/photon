package de.komoot.photon;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigSynonyms {

    private List<String> searchSynonyms = null;
    private List<ConfigClassificationTerm> classificationTerms = List.of();

    public List<String> getSearchSynonyms() {
        return searchSynonyms;
    }

    @JsonProperty("search_synonyms")
    public void setSearchSynonyms(List<String> searchSynonyms) {
        this.searchSynonyms = searchSynonyms;
    }

    public List<ConfigClassificationTerm> getClassificationTerms() {
        return classificationTerms;
    }

    @JsonProperty("classification_terms")
    public void setClassificationTerms(List<ConfigClassificationTerm> classificationTerms) {
        this.classificationTerms = classificationTerms.stream()
                .filter(ConfigClassificationTerm::isValidCategory)
                .collect(Collectors.toList());
    }
}

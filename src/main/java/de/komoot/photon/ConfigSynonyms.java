package de.komoot.photon;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ConfigSynonyms {

    private List<String> searchSynonyms = null;
    private ConfigClassificationTerm[] classificationTerms = null;

    public List<String> getSearchSynonyms() {
        return searchSynonyms;
    }

    @JsonProperty("search_synonyms")
    public void setSearchSynonyms(List<String> searchSynonyms) {
        this.searchSynonyms = searchSynonyms;
    }

    public ConfigClassificationTerm[] getClassificationTerms() {
        return classificationTerms;
    }

    @JsonProperty("classification_terms")
    public void setClassificationTerms(ConfigClassificationTerm[] classificationTerms) {
        this.classificationTerms = classificationTerms;
    }
}

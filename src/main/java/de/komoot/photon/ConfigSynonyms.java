package de.komoot.photon;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@NullMarked
public class ConfigSynonyms {

    @Nullable private List<String> searchSynonyms = null;
    private List<ConfigClassificationTerm> classificationTerms = List.of();

    @Nullable
    public List<String> getSearchSynonyms() {
        return searchSynonyms;
    }

    @JsonProperty("search_synonyms")
    @SuppressWarnings("unused")
    public void setSearchSynonyms(List<String> searchSynonyms) {
        this.searchSynonyms = searchSynonyms;
    }

    public List<ConfigClassificationTerm> getClassificationTerms() {
        return classificationTerms;
    }

    @JsonProperty("classification_terms")
    @SuppressWarnings("unused")
    public void setClassificationTerms(List<ConfigClassificationTerm> classificationTerms) {
        this.classificationTerms = classificationTerms.stream()
                .filter(ConfigClassificationTerm::isValidCategory)
                .collect(Collectors.toList());
    }
}

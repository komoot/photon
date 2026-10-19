package de.komoot.photon;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
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
    public void setSearchSynonyms(List<String> searchSynonyms) {
        for (var synonym : searchSynonyms) {
            if (synonym.matches(".*[ '-].*")) {
                throw new UsageException("Error in synonym entry '" + synonym +
                                         "'. Terms must not contain spaces or hyphens.");
            }
        }
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

    public static ConfigSynonyms loadFromFile(String synonymFile) {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                .build()
                .readValue(new File(synonymFile), ConfigSynonyms.class);
    }
}

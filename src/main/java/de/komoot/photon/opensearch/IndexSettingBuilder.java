package de.komoot.photon.opensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.ConfigClassificationTerm;
import de.komoot.photon.ConfigSynonyms;
import de.komoot.photon.UsageException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.analysis.TokenChar;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class IndexSettingBuilder {
    private IndexSettingsAnalysis.Builder settings = new IndexSettingsAnalysis.Builder();
    private int numShards = 1;
    private Set<String> extraFilters = new HashSet<>();

    public IndexSettingBuilder setShards(Integer numShards) {
        this.numShards = numShards == null ? 1 : numShards;
        return this;
    }

    public void createIndex(OpenSearchClient client, String indexName) throws IOException {
        addDefaultSettings();

        client.indices().create(r -> r
                .index(indexName)
                .settings(s -> s
                        .numberOfShards(Integer.toString(numShards))
                        .analysis(settings.build())));
    }

    public void updateIndex(OpenSearchClient client, String indexName) throws IOException {
        addDefaultSettings();

        client.indices().close(req -> req.index(indexName));
        try {
            client.indices().putSettings(req -> req
                    .index(indexName)
                    .settings(s -> s.analysis(settings.build())));
        } finally {
            client.indices().open(req -> req.index(indexName));
        }
    }

    public IndexSettingBuilder setSynonymFile(String synonymFile) throws IOException {
        if (synonymFile != null) {
            final var synonymConfig = new ObjectMapper().readValue(new File(synonymFile), ConfigSynonyms.class);

            if (synonymConfig.getSearchSynonyms() != null) {
                insertSynonymFilter("extra_synonyms", synonymConfig.getSearchSynonyms());
            }
            if (synonymConfig.getClassificationTerms() != null) {
                setClassificationTerms(synonymConfig.getClassificationTerms());
            }
        }
        return this;
    }

    private void setClassificationTerms(ConfigClassificationTerm[] terms) {
        // Collect for each term in the list the possible classification expansions.
        Map<String, Set<String>> collector = new HashMap<>();
        for (var term : terms) {
            String classString = term.getClassificationString();

            if (classString != null) {
                for (var repl : term.getTerms()) {
                    String norm = repl.toLowerCase().trim();
                    if (norm.indexOf(' ') >= 0) {
                        throw new UsageException("Syntax error in synonym file: only single word classification terms allowed.");
                    }

                    if (norm.length() > 1) {
                        collector.computeIfAbsent(norm, k -> new HashSet<>()).add(classString);
                    }
                }
            }
        }

        // Create the final list of synonyms. A term can expand to any classificator or not at all.
        List<String> synonyms = new ArrayList<>();
        collector.forEach((term, classificators) ->
                synonyms.add(term + " => " + term + "," + String.join(",", classificators)));

        insertSynonymFilter("classification_synonyms", synonyms);

    }

    private void insertSynonymFilter(String filterName, List<String> synonyms) {
        if (!synonyms.isEmpty()) {
            settings.filter(filterName,
                    f -> f.definition(d -> d
                            .synonymGraph(s -> s.synonyms(synonyms))));

            extraFilters.add(filterName);
        }
    }

    private void addDefaultSettings() {
        settings.filter("photonlength",
                f -> f.definition(d -> d.length(l -> l.min(2).max(500))));
        settings.filter("preserving_word_delimiter",
                f -> f.definition(d -> d.wordDelimiterGraph(w -> w.preserveOriginal(true))));

        settings.charFilter("punctuationgreedy",
                f -> f.definition(d -> d.patternReplace(p -> p.pattern("[\\.,']").replacement(" "))));
        settings.charFilter("remove_ws_hnr_suffix",
                f -> f.definition(d -> d.patternReplace(p -> p.pattern("(\\d+)\\s(?=\\p{L}\\b)").replacement("$1"))));

        settings.tokenizer("edge_ngram",
                f -> f.definition(d -> d.edgeNgram(e -> e.minGram(1).maxGram(100).tokenChars(TokenChar.Letter, TokenChar.Digit))));

        settings.analyzer("index_ngram",
                f -> f.custom(d -> d
                        .charFilter("punctuationgreedy", "remove_ws_hnr_suffix")
                        .tokenizer("edge_ngram")
                        .filter("preserving_word_delimiter",
                                "flatten_graph",
                                "lowercase",
                                "german_normalization",
                                "asciifolding",
                                "unique")));
        settings.analyzer("search",
                f -> f.custom(d -> {
                    d.charFilter("punctuationgreedy")
                            .tokenizer("standard")
                            .filter("lowercase");
                    for (var filter : extraFilters) {
                        d.filter(filter);
                    }
                    d.filter("german_normalization", "asciifolding");

                    return d;
                }));
        settings.analyzer("index_raw",
                f -> f.custom(d -> d
                        .charFilter("punctuationgreedy")
                        .tokenizer("standard")
                        .filter("lowercase",
                                "german_normalization",
                                "asciifolding",
                                "unique")));
        settings.analyzer("index_housenumber",
                f -> f.custom(d -> d
                        .charFilter("punctuationgreedy", "remove_ws_hnr_suffix")
                        .tokenizer("standard")
                        .filter("lowercase",
                                "preserving_word_delimiter")));
        settings.analyzer("search_classification",
                f -> f.custom(d -> {
                    d.tokenizer("whitespace")
                            .filter("lowercase");
                    if (extraFilters.contains("classification_synonyms")) {
                        d.filter("classification_synonyms");
                    }

                    return d;
                }));
    }
}

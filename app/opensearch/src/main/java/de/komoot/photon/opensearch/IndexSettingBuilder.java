package de.komoot.photon.opensearch;

import de.komoot.photon.Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.analysis.TokenChar;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        client.indices().putSettings(req -> req
                .index(indexName)
                .settings(s -> s.analysis(settings.build())));
        client.indices().open(req -> req.index(indexName));
    }

    public IndexSettingBuilder setSynonymFile(String synonymFile) throws IOException {
        if (synonymFile != null) {
            final var synonymConfig = new JSONObject(new JSONTokener(new FileReader(synonymFile)));

            setSearchTimeSynonyms(synonymConfig.optJSONArray("search_synonyms"));
            setClassificationTerms(synonymConfig.optJSONArray("classification_terms"));
        }
        return this;
    }

    private void setSearchTimeSynonyms(JSONArray synonyms) {
        if (synonyms != null) {
            insertSynonymFilter("extra_synonyms", synonyms);
        }
    }

    private void setClassificationTerms(JSONArray terms) {
        if (terms == null) {
            return;
        }

        // Collect for each term in the list the possible classification expansions.
        Map<String, Set<String>> collector = new HashMap<>();
        for (int i = 0; i < terms.length(); i++) {
            JSONObject descr = terms.getJSONObject(i);

            String classString = Utils.buildClassificationString(descr.getString("key"), descr.getString("value")).toLowerCase();

            if (classString != null) {
                JSONArray jsonTerms = descr.getJSONArray("terms");
                for (int j = 0; j < jsonTerms.length(); j++) {
                    String term = jsonTerms.getString(j).toLowerCase().trim();
                    if (term.indexOf(' ') >= 0) {
                        throw new RuntimeException("Syntax error in synonym file: only single word classification terms allowed.");
                    }

                    if (term.length() > 1) {
                        collector.computeIfAbsent(term, k -> new HashSet<>()).add(classString);
                    }
                }
            }
        }

        // Create the final list of synonyms. A term can expand to any classificator or not at all.
        JSONArray synonyms = new JSONArray();
        collector.forEach((term, classificators) ->
                synonyms.put(term + " => " + term + "," + String.join(",", classificators)));

        insertSynonymFilter("classification_synonyms", synonyms);

    }

    private void insertSynonymFilter(String filterName, JSONArray synonyms) {
        if (!synonyms.isEmpty()) {
            settings.filter(filterName,
                    f -> f.definition(d -> d
                            .synonymGraph(s -> {
                                for (var ele : synonyms) {
                                    s.synonyms(ele.toString());
                                }
                                return s;
                            })));

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

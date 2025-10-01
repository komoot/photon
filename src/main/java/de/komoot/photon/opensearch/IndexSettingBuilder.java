package de.komoot.photon.opensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.ConfigClassificationTerm;
import de.komoot.photon.ConfigSynonyms;
import de.komoot.photon.UsageException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class IndexSettingBuilder {
    private final IndexSettingsAnalysis.Builder settings = new IndexSettingsAnalysis.Builder();
    private int numShards = 1;
    private final Set<String> extraFilters = new HashSet<>();

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
        final var NORMALIZATION_FILTERS = List.of(
                "lowercase",
                "german_normalization",
                "asciifolding"
        );

        settings.charFilter("punctuationgreedy",
                f -> f.definition(d -> d.patternReplace(p -> p.pattern("[\\.,']").replacement(" "))));

        settings.analyzer("search", f -> f.custom(d -> {
            d.charFilter("punctuationgreedy");
            d.tokenizer("standard");
            d.filter(NORMALIZATION_FILTERS);
            for (var filter : extraFilters) {
                d.filter(filter);
            }

            return d;
        }));

        settings.analyzer("index_raw", f -> f.custom(d -> d
                .charFilter("punctuationgreedy")
                .tokenizer("standard")
                .filter(NORMALIZATION_FILTERS)
        ));

        settings.analyzer("search_prefix", f -> f.custom(d -> d
                .tokenizer("keyword")
                .filter("keep_alphanum")
                .filter(NORMALIZATION_FILTERS)
        ));

        // Collector analyzers.
        settings.tokenizer("collection_split", b -> b.definition(d -> d
                .simplePatternSplit(p -> p.pattern(";"))
        ));

        settings.filter("delimiter_whitespace", f -> f.definition(d -> d
                .wordDelimiterGraph(w -> w
                        .preserveOriginal(false)
                        .stemEnglishPossessive(false)
                        .splitOnNumerics(false)
                        .splitOnCaseChange(false)
                        .typeTable("- => ALPHA",
                                "' => ALPHA",
                                ". => ALPHA"))
        ));

        settings.filter("delimiter_terms", f -> f.definition(d -> d
                .wordDelimiterGraph(w -> w
                        .preserveOriginal(false)
                        .stemEnglishPossessive(false)
                        .catenateAll(true))
        ));

        settings.filter("delimiter_alphanum", f -> f.definition(d -> d
                .wordDelimiterGraph(w -> w
                        .preserveOriginal(false)
                        .stemEnglishPossessive(false)
                        .splitOnNumerics(false)
                        .splitOnCaseChange(false))
        ));

        settings.filter("keep_alphanum", f -> f.definition(d -> d
                .patternReplace(p -> p
                        .pattern("[^\\p{IsAlphabetic}\\p{IsDigit}]")
                        .replacement("")
                        .flags(""))
        ));

        settings.filter("prefix_edge_ngram", f -> f.definition(d -> d
                .edgeNgram(e -> e
                        .minGram(1)
                        .maxGram(30)
                        .preserveOriginal(false))
        ));

        settings.filter("name_edge_ngram", f -> f.definition(d -> d
                .edgeNgram(e -> e
                        .minGram(5)
                        .maxGram(30)
                        .preserveOriginal(true))
        ));


        settings.analyzer("index_fullword", f -> f.custom(d -> d
                .tokenizer("collection_split")
                .filter("delimited_term_freq",
                        "delimiter_whitespace",
                        "delimiter_terms")
                .filter(NORMALIZATION_FILTERS)
                .filter("unique")
        ));

        settings.analyzer("index_ngram", f -> f.custom(d -> d
                .tokenizer("collection_split")
                .filter("delimited_term_freq",
                        "delimiter_whitespace",
                        "delimiter_terms",
                        "prefix_edge_ngram")
                .filter(NORMALIZATION_FILTERS)
                .filter("unique")
        ));

        settings.analyzer("index_name_ngram", f -> f.custom(d -> d
                .tokenizer("collection_split")
                .filter("delimited_term_freq",
                        "delimiter_whitespace",
                        "delimiter_terms",
                        "name_edge_ngram")
                .filter(NORMALIZATION_FILTERS)
                .filter("unique")
        ));

        settings.analyzer("index_name_prefix", f -> f.custom(d -> d
                .tokenizer("collection_split")
                .filter("delimited_term_freq",
                        "keep_alphanum")
                .filter(NORMALIZATION_FILTERS)
                .filter("prefix_edge_ngram", "unique")
        ));

        settings.analyzer("index_housenumber", f -> f.custom(d -> d
                .tokenizer("collection_split")
                .filter("lowercase",
                        "delimiter_alphanum",
                        "delimiter_terms")));

        settings.analyzer("lowercase_keyword", f -> f.custom(d -> d
                .tokenizer("keyword")
                .filter("lowercase")
        ));
    }
}

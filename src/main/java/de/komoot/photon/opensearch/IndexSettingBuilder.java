package de.komoot.photon.opensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.ConfigClassificationTerm;
import de.komoot.photon.ConfigSynonyms;
import de.komoot.photon.UsageException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.analysis.CustomAnalyzer;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class IndexSettingBuilder {
    private static final String SYNONYM_FILTER = "extra_synonyms";
    private static final String CLASSIFICATION_FILTER = "classification_synonyms";
    private final IndexSettingsAnalysis.Builder settings = new IndexSettingsAnalysis.Builder();
    private int numShards = 1;
    private boolean hasSynonymFilter = false;
    private boolean hasClassificationFilter = false;

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

            final var synonyms = synonymConfig.getSearchSynonyms();
            if (synonyms != null && !synonyms.isEmpty()) {
                settings.filter(SYNONYM_FILTER, f -> f.definition(d -> d
                        .synonymGraph(s -> s.synonyms(synonyms))
                ));
                hasSynonymFilter = true;
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
        if (!collector.isEmpty()) {
            final var synonyms = collector.entrySet().stream()
                    .map(e -> String.join(" => ", e.getKey(), String.join(",", e.getValue())))
                    .collect(Collectors.toList());

            settings.filter(CLASSIFICATION_FILTER, f -> f.definition(d -> d
                    .synonymGraph(s -> s.synonyms(synonyms))
            ));
            hasClassificationFilter = true;
        }
    }

    private CustomAnalyzer buildSearchAnalyzer(List<String> normFilters) {
        final var builder = new CustomAnalyzer.Builder();

        settings.tokenizer("search_tokenizer", t -> t.definition(d -> d
                .simplePatternSplit(p -> p
                        .pattern("[ ,!?;]+"))
        ));

        settings.filter("delimiter_search", f -> f.definition(d -> d
                .wordDelimiterGraph(w -> w
                        .splitOnNumerics(false)
                        .stemEnglishPossessive(false)
                        .preserveOriginal(false))
        ));

        builder.charFilter("punctuationgreedy");
        builder.tokenizer("search_tokenizer");
        builder.filter(normFilters);

        List<String> extraFilters = new ArrayList<>();
        if (hasSynonymFilter) {
            extraFilters.add(SYNONYM_FILTER);
        }
        extraFilters.add("delimiter_search");

        settings.filter("multiplexer_search", f -> f.definition(d -> d
                .multiplexer(m -> m
                        .preserveOriginal(false)
                        .filters("drop_classification,drop_empty_tokens,"
                                + String.join(",", extraFilters))
                        .filters((hasClassificationFilter ? CLASSIFICATION_FILTER + "," : "")
                                + "keep_classification,drop_empty_tokens"))
        ));
        builder.filter("multiplexer_search");

        return builder.build();
    }

    private CustomAnalyzer buildClassificationAnalyser(String name, List<String> normFilters, List<String> extraFilters) {
        final var builder = new CustomAnalyzer.Builder();

        settings.filter("multiplexer_" + name, f -> f.definition(d -> d
                .multiplexer(m -> m
                        .preserveOriginal(false)
                        .filters("keep_classification,drop_empty_tokens,split_classification,"
                                        + String.join(",", normFilters),
                        "drop_classification,delimiter_whitespace,delimiter_terms,"
                                + String.join(",", normFilters)
                                + (extraFilters.isEmpty() ? "" : ",")
                                + String.join(",", extraFilters))
                )
        ));

        builder.tokenizer("collection_split");
        builder.filter("delimited_term_freq", "multiplexer_" + name, "drop_empty_tokens", "unique");

        return builder.build();
    }

    private void addDefaultSettings() {
        final var NORMALIZATION_FILTERS = List.of(
                "lowercase",
                "german_normalization",
                "asciifolding"
        );

        // Classification filtering.
        settings.filter("keep_classification", f -> f.definition(d -> d
                .patternReplace(p -> p
                        .pattern("^[^#].*$")
                        .replacement("")
                        .flags(""))
        ));

        settings.filter("drop_classification", f -> f.definition(d -> d
                .patternReplace(p -> p
                        .pattern("^#.*$")
                        .replacement("")
                        .flags(""))
        ));

        settings.filter("split_classification", f -> f.definition(d -> d
                .patternCapture(c -> c
                        .preserveOriginal(false)
                        .patterns("^(#[^.]+\\.[^.]+)",
                                "^(#[^.]+\\.[^.]+\\.[^.]+)",
                                "^(#[^.]+\\.[^.]+\\.[^.]+\\.[^.]+)",
                                "^(#[^.]+\\.[^.]+\\.[^.]+\\.[^.]+\\.[^.]+)")
                )
        ));

        settings.filter("split_category", f -> f.definition(d -> d
                .patternCapture(c -> c
                        .preserveOriginal(false)
                        .patterns("^([^.]+\\.[^.]+)",
                                "^([^.]+\\.[^.]+\\.[^.]+)",
                                "^([^.]+\\.[^.]+\\.[^.]+\\.[^.]+)",
                                "^([^.]+\\.[^.]+\\.[^.]+\\.[^.]+\\.[^.]+)")
                )
        ));

        settings.filter("drop_empty_tokens", f -> f.definition(d -> d
                .length(l -> l.min(1).max(500))
        ));

        // Search analyzers
        settings.charFilter("punctuationgreedy", f -> f.definition(d -> d
                .patternReplace(p -> p.pattern("'").replacement(" "))
        ));

        settings.filter("keep_alphanum", f -> f.definition(d -> d
                .patternReplace(p -> p
                        .pattern("[^\\p{IsAlphabetic}\\p{IsDigit}]")
                        .replacement("")
                        .flags(""))
        ));

        settings.analyzer("search", f -> f.custom(buildSearchAnalyzer(NORMALIZATION_FILTERS)));

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


        settings.analyzer("index_fullword", f -> f.custom(
                buildClassificationAnalyser("fullword", NORMALIZATION_FILTERS, List.of())));

        settings.analyzer("index_ngram", f -> f.custom(
                buildClassificationAnalyser("ngram", NORMALIZATION_FILTERS, List.of("prefix_edge_ngram"))
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

        settings.analyzer("index_raw", f -> f.custom(d -> d
                .charFilter("punctuationgreedy")
                .tokenizer("standard")
                .filter(NORMALIZATION_FILTERS)
        ));

        settings.analyzer("index_categories", f -> f.custom(d -> d
                .tokenizer("keyword")
                .filter("split_category")
        ));
    }
}

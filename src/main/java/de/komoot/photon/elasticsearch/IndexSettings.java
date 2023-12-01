package de.komoot.photon.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.Utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Encapsulates the ES index settings for the photon index. Adds functions to
 * manipulate and apply the settings.
 *
 */
public class IndexSettings {
    private static final ObjectMapper objMapper = new ObjectMapper();

    /**
     * Build index settings
     * Add query-time synonyms and classification terms from a file.
     *
     * Synonyms need to be supplied in a simple text file with one synonym entry per line.
     * Synonyms need to be comma-separated. Only single-term synonyms are supported at this
     * time. Spaces in the synonym list are considered a syntax error.
     *
     * @param synonymFilePath File containing the synonyms.
     *
     * @return Index settings as an ObjectNode
     */
    public static ObjectNode buildSettings(String synonymFilePath) throws IOException {
        ArrayNode synonyms = null;
        ArrayNode classSynonyms = null;

        if (synonymFilePath != null) {
            ObjectNode synonymConfig = objMapper.readValue(new FileReader(synonymFilePath), ObjectNode.class);

            if (synonymConfig.hasNonNull("search_synonyms")) {
                synonyms = (ArrayNode) synonymConfig.get("search_synonyms");
            }

            if (synonymConfig.hasNonNull("classification_terms")) {
                ArrayNode terms = (ArrayNode) synonymConfig.get("classification_terms");
                classSynonyms = collectClassificationTerms(terms);
            }
        }

        return objMapper.createObjectNode()
                .putPOJO("analysis", objMapper.createObjectNode()
                    .putPOJO("analyzer", buildAnalyzer(synonyms, classSynonyms))
                    .putPOJO("tokenizer", buildTokenizer())
                    .putPOJO("char_filter", buildCharFilter())
                    .putPOJO("filter", buildFilter(synonyms, classSynonyms))
                );
    }

    public static ObjectNode setShards(ObjectNode settings, Integer numShards) {
        return settings.putPOJO("index", objMapper
                .createObjectNode()
                .put("number_of_shards", numShards)
        );
    }

    public static ObjectNode unsetShards(ObjectNode settings) {
        return (ObjectNode) settings.remove("index");
    }

    private static ObjectNode buildAnalyzer(ArrayNode synonyms, ArrayNode classSynonyms) {
        ObjectNode indexNgram = objMapper.createObjectNode()
                .put("tokenizer", "edge_ngram")
                .putPOJO("char_filter", objMapper
                        .createArrayNode()
                        .add("punctuationgreedy")
                        .add("remove_ws_hnr_suffix")
                )
                .putPOJO("filter", objMapper
                        .createArrayNode()
                        .add("preserving_word_delimiter")
                        .add("lowercase")
                        .add("german_normalization")
                        .add("asciifolding")
                        .add("unique")
                );

        ObjectNode indexRaw = objMapper.createObjectNode()
                .put("tokenizer", "standard")
                .putPOJO("char_filter", objMapper
                        .createArrayNode()
                        .add("punctuationgreedy")
                )
                .putPOJO("filter", objMapper
                        .createArrayNode()
                        .add("word_delimiter")
                        .add("lowercase")
                        .add("german_normalization")
                        .add("asciifolding")
                        .add("unique")
                );

        ObjectNode indexHousenumber = objMapper.createObjectNode()
                .put("tokenizer", "standard")
                .putPOJO("char_filter", objMapper
                        .createArrayNode()
                        .add("punctuationgreedy")
                        .add("remove_ws_hnr_suffix")
                )
                .putPOJO("filter", objMapper
                        .createArrayNode()
                        .add("lowercase")
                        .add("preserving_word_delimiter")
                );

        ArrayNode searchClassificationFilter = objMapper.createArrayNode().add("lowercase");
        ArrayNode searchNgramFilter = objMapper.createArrayNode().add("lowercase");
        ArrayNode searchRawFilter = objMapper.createArrayNode().add("word_delimiter").add("lowercase");

        if (classSynonyms != null && !classSynonyms.isEmpty()) {
            searchClassificationFilter.add("classification_synonyms");
            searchNgramFilter.add("classification_synonyms");
            searchRawFilter.add("classification_synonyms");
        }

        if (synonyms != null && !synonyms.isEmpty()) {
            searchNgramFilter.add("extra_synonyms");
            searchRawFilter.add("extra_synonyms");
        }

        ObjectNode searchClassification = objMapper.createObjectNode()
                .put("tokenizer", "whitespace")
                .putPOJO("filter", searchClassificationFilter);


        ObjectNode searchNgram = objMapper.createObjectNode()
                .put("tokenizer", "standard")
                .putPOJO("char_filter", objMapper
                        .createArrayNode()
                        .add("punctuationgreedy")
                )
                .putPOJO("filter", searchNgramFilter
                        .add("german_normalization")
                        .add("asciifolding")
                );

        ObjectNode searchRaw = objMapper.createObjectNode()
                .put("tokenizer", "standard")
                .putPOJO("char_filter", objMapper
                        .createArrayNode()
                        .add("punctuationgreedy")
                )
                .putPOJO("filter", searchRawFilter
                        .add("german_normalization")
                        .add("asciifolding")
                        .add("unique")
                );


        return objMapper.createObjectNode()
                .putPOJO("index_ngram", indexNgram)
                .putPOJO("search_ngram", searchNgram)
                .putPOJO("index_raw", indexRaw)
                .putPOJO("search_raw", searchRaw)
                .putPOJO("index_housenumber", indexHousenumber)
                .putPOJO("search_classification", searchClassification);
    }

    private static ObjectNode buildTokenizer() {
        return objMapper.createObjectNode()
                .putPOJO("edge_ngram", objMapper
                        .createObjectNode()
                        .put("type", "edge_ngram")
                        .put("min_gram", 1)
                        .put("max_gram", 100)
                        .putPOJO("token_chars", objMapper
                                .createArrayNode()
                                .add("letter")
                                .add("digit")
                        )
                );
    }

    private static ObjectNode buildCharFilter() {
        return objMapper.createObjectNode()
                .putPOJO("punctuationgreedy", objMapper
                        .createObjectNode()
                        .put("type", "pattern_replace")
                        .put("pattern", "[\\.,']")
                        .put("replacement", " ")
                )
                .putPOJO("remove_ws_hnr_suffix", objMapper
                        .createObjectNode()
                        .put("type", "pattern_replace")
                        .put("pattern", "(\\d+)\\s(?=\\p{L}\\b)")
                        .put("replacement", "$1")
                );
    }

    private static ObjectNode buildFilter(ArrayNode synonyms, ArrayNode classTerms) {
        ObjectNode filter = objMapper.createObjectNode()
                .putPOJO("photonlength", objMapper
                        .createObjectNode()
                        .put("min", 2)
                        .put("type", "length")
                )
                .putPOJO("preserving_word_delimiter", objMapper
                        .createObjectNode()
                        .put("type", "word_delimiter_graph")
                        .put("preserve_original", true)
                );

        if (synonyms != null && !synonyms.isEmpty()) {
            ObjectNode extraSynonyms = objMapper.createObjectNode()
                    .put("type", "synonym")
                    .putPOJO("synonyms", synonyms);

            filter.putPOJO("extra_synonyms", extraSynonyms);
        }

        if (classTerms != null && !classTerms.isEmpty()) {
            ObjectNode classSynonyms = objMapper.createObjectNode()
                    .put("type", "synonym")
                    .putPOJO("synonyms", classTerms);

            filter.putPOJO("classification_synonyms", classSynonyms);
        }

        return filter;
    }

    private static ArrayNode collectClassificationTerms(ArrayNode terms) {
        if (terms == null) {
            return null;
        }

        Map<String, Set<String>> collector = new HashMap<>();
        for (JsonNode termObject : terms) {
            String classString = Utils.buildClassificationString(
                    termObject.get("key").asText(),
                    termObject.get("value").asText()
            );

            if (classString != null)  {
                classString = classString.toLowerCase();
                ArrayNode jsonTerms = (ArrayNode) termObject.get("terms");
                for (JsonNode term : jsonTerms) {
                    String termString = term.asText().toLowerCase().trim();
                    if (termString.indexOf(' ') >= 0) {
                        throw new RuntimeException("Syntax error in synonym file: only single word classification terms allowed.");
                    }

                    if (termString.length() > 1) {
                        collector.computeIfAbsent(termString, k -> new HashSet<>()).add(classString);
                    }
                }
            }
        }

        ArrayNode synonyms = objMapper.createArrayNode();
        collector.forEach(
                (term, classifier) -> synonyms.add(term + " => " + term + "," + String.join(",", classifier))
        );
        return synonyms;
    }

}

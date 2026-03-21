package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@NullMarked
public class QueryReranker implements Consumer<PhotonResult> {
    private static final Pattern WORD_BREAK_PATTERN = Pattern.compile("[-,: ]+");
    private final String query;
    private final String language;

    public QueryReranker(String query, String language) {
        this.query = normalize(query);
        this.language = language;
    }

    @Override
    public void accept(PhotonResult result) {
        result.adjustScore(rescore(result));
    }

    private double rescore(PhotonResult result) {
        var localeName = result.getLocalised("name", language, GeoJsonFormatter.NAME_PRECEDENCE);
        if (localeName != null) {
            localeName = normalize(localeName);
            if (query.equals(localeName)) {
                return 1.0;
            }
            if (localeName.startsWith(query)) {
                if (localeName.charAt(query.length()) == ' ') {
                    return 0.9;
                }
                return Math.min(0.7, (double) query.length() / localeName.length());
            }
        }

        // address parts, we want to rematch in the query
        var resultTerms = Stream.of(
                        localeName,
                        (String) result.get(DocFields.HOUSENUMBER),
                        result.getLocalised(DocFields.STREET, language),
                        result.getLocalised(DocFields.CITY, language),
                        (String) result.get(DocFields.COUNTRYCODE),
                        (String) result.get(DocFields.POSTCODE),
                        result.getLocalised(DocFields.COUNTRY, language),
                        result.getLocalised(DocFields.STATE, language),
                        result.getLocalised(DocFields.COUNTY, language),
                        result.getLocalised(DocFields.DISTRICT, language))
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .toList();

        double matches = 0.0;
        var todo = new StringBuilder(" " + query + " ");
        var rematchWords = new ArrayList<String>();
        // first try to match the full words, keep address parts that do not match
        for (var term : resultTerms) {
            var idx = todo.indexOf(" " + term);
            if (idx >= 0) {
                if (todo.charAt(idx + term.length() + 1) == ' ') {
                    matches += term.length();
                    todo.delete(idx + 1, idx + term.length() + 2);
                    if (todo.toString().isBlank()) {
                        return matches / query.length();
                    }
                    continue;
                }
            }
            rematchWords.addAll(Arrays.asList(term.split(" ")));
        }

        // still query left to do, try prefix matching on remaining parts
        matches += Arrays.stream(todo.toString().strip().split(" +"))
                .mapToDouble(w -> {
                    for (var term : rematchWords) {
                        if (term.startsWith(w)) {
                            return Double.min(0.7, (double) w.length() / term.length()) * w.length();
                        }
                    }
                    return 0.0;
                }).sum();

        return matches / query.length();
    }

    private String normalize(String in) {
        return WORD_BREAK_PATTERN.matcher(in.toLowerCase()).replaceAll(" ");
    }
}

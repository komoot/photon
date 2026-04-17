package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@NullMarked
public class QueryReranker implements Consumer<PhotonResult> {
    private static final Pattern WORD_BREAK_PATTERN = Pattern.compile("[-,: ]+");
    private final String query;
    private final String language;
    private final boolean isMultiTermQuery;
    private final boolean isFullQuery;

    public QueryReranker(String query, String language) {
        this.query = normalize(query);
        this.language = language;
        this.isMultiTermQuery = query.indexOf(',') >= 0;
        this.isFullQuery = query.endsWith(" ");
    }

    @Override
    public void accept(PhotonResult result) {
        if (!query.isEmpty()) {
            result.adjustScore(rescore(result));
        }
    }

    private double rescore(PhotonResult result) {
        var localeName = result.getLocalised("name", language, GeoJsonFormatter.NAME_PRECEDENCE);
        if (!isMultiTermQuery && localeName != null) {
            localeName = normalize(localeName);
            if (query.equals(localeName)) {
                return 1.0;
            }
            if (localeName.startsWith(query)) {
                if (localeName.charAt(query.length()) == ' ') {
                    return 0.9;
                }
                if (!isFullQuery) {
                    return 0.8;
                }
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
                        result.getLocalised(DocFields.DISTRICT, language),
                        result.getLocalised(DocFields.NAME, "default"),
                        result.getLocalised(DocFields.NAME, "alt"))
                .mapMulti(this::mapNames)
                .toList();

        double matches = 0.0;
        var todo = new StringBuilder(" " + query + " ");
        var rematchWords = new ArrayList<String>();
        // first try to match the full words, keep address parts that do not match
        for (var term : resultTerms) {
            var idx = todo.indexOf(" " + term + " ");
            if (idx >= 0) {
                matches += term.length();
                todo.delete(idx + 1, idx + term.length() + 2);
                if (todo.toString().isBlank()) {
                    return 0.8 * matches / query.length();
                }
                continue;
            }
            rematchWords.addAll(Arrays.asList(term.split(" ")));
        }

        // still query left to do, try prefix matching on remaining parts
        for (var w : todo.toString().strip().split(" +")) {
            for (var term : rematchWords) {
                if (term.startsWith(w)) {
                    matches += 0.7 * w.length();
                    break;
                }
            }
        }

        return 0.8 * matches / query.length();
    }

    private String normalize(String in) {
        return WORD_BREAK_PATTERN.matcher(in.toLowerCase()).replaceAll(" ").strip();
    }

    private void mapNames(@Nullable String in, Consumer<String> consumer) {
        if (in != null) {
            for (var s : in.split(";")) {
                var finname = normalize(s);
                if (!finname.isEmpty()) {
                    consumer.accept(finname);
                }
            }
        }
    }
}

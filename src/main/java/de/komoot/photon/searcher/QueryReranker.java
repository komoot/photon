package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@NullMarked
public class QueryReranker implements Consumer<PhotonResult> {
    private static final Pattern WORD_BREAK_PATTERN = Pattern.compile("[-,: ]+");
    private final String query;
    private final String language;
    @Nullable private final String fallbackLanguage;
    private final boolean isMultiTermQuery;
    private final boolean isFullQuery;

    public QueryReranker(String query, String language, @Nullable String fallbackLanguage) {
        this.query = normalize(query);
        this.language = language;
        this.fallbackLanguage = fallbackLanguage;
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
        var localeName = result.getLocalisedWithFallback("name", language, fallbackLanguage, GeoJsonFormatter.NAME_PRECEDENCE);
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

        double matches = 0.0;
        var todo = new StringBuilder(" " + query + " ");
        var resultTerms = new ArrayList<String>();

        var hnrMatchScore = matchHousenumber(todo, result);
        var streetName = result.getLocalised(DocFields.STREET, language);
        if (hnrMatchScore > 0.0) {
            matches += hnrMatchScore;
            matches += matchMainTerm(todo, streetName);
            if (localeName != null) {
                resultTerms.add(localeName);
            }
            resultTerms.addAll(secondaryNames(result));
        } else {
            var mainMatch = matchMainTerm(todo, localeName);
            for (var name: secondaryNames(result)) {
                if (mainMatch > 0.0) {
                    break;  // only one of the names can be a match
                }
                mainMatch = matchMainTerm(todo, name) * 0.9;
            }
            matches += mainMatch;
            if (streetName != null) {
                resultTerms.add(streetName);
            }
        }

        matches += matchCountry(todo, result);

        if (todo.toString().isBlank()) {
            return 0.8 * matches / query.length();
        }

        // address parts, we want to rematch in the query
        mapNames(result.getLocalised(DocFields.CITY, language), resultTerms);
        mapNames(result.getLocalised(DocFields.STATE, language), resultTerms);
        mapNames(result.getLocalised(DocFields.COUNTY, language), resultTerms);
        mapNames(result.getLocalised(DocFields.DISTRICT, language), resultTerms);
        mapNames((String) result.get(DocFields.POSTCODE), resultTerms);

        var rematchWords = new ArrayList<String>();
        // first try to match the full words, keep address parts that do not match
        for (var term : resultTerms) {
            var idx = todo.indexOf(" " + term + " ");
            if (idx >= 0) {
                matches += 0.8 * term.length();
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
                    matches += 0.4 * w.length();
                    break;
                }
            }
        }

        if (matches == 0.0) {
            // Not matching at all, still give it a slight boost when it is important.
            return 0.5 * result.getImportance();
        }

        return 0.8 * matches / query.length();
    }

    private double matchMainTerm(StringBuilder todo, @Nullable String name) {
        if (name == null) {
            return 0.0;
        }

        var idx = todo.indexOf(" " + name + " ");
        if (idx >= 0) {
            todo.delete(idx + 1, idx + name.length() + 2);
            return name.length();
        }

        var rematchWords = name.split(" +");
        double matches = 0.0;

        if (rematchWords.length > 1) {
            for (var term : rematchWords) {
                idx = todo.indexOf(" " + term + " ");
                if (idx >= 0) {
                    todo.delete(idx + 1, idx + term.length() + 2);
                    matches += term.length();
                }
            }
        }

        return matches * 0.6;
    }

    private double matchHousenumber(StringBuilder todo, PhotonResult result) {
        var hnr = (String) result.get(DocFields.HOUSENUMBER);

        if (hnr != null) {
            var norm = normalize(hnr);
            var idx = todo.indexOf(" " + norm + " ");
            if (idx > 0) {
                todo.delete(idx + 1, idx + norm.length() + 2);
                return norm.length();
            }

            var spaceIdx = norm.indexOf(' ');
            if (spaceIdx > 0) {
                idx = todo.indexOf(" " + norm.substring(0, spaceIdx + 1));
                if (idx > 0) {
                    todo.delete(idx + 1, idx + spaceIdx + 1);
                    return 0.5 * spaceIdx;
                }
            }
        }

        return 0.0;
    }

    private double matchCountry(StringBuilder todo, PhotonResult result) {
        var cc = (String) result.get(DocFields.COUNTRYCODE);
        if (cc != null) {
            var idx = todo.indexOf(" " + cc + " ");
            if (idx == 0 || idx + cc.length() + 2 >= todo.length()) {
                todo.delete(idx + 1, idx + cc.length() + 2);
                return cc.length();
            }
        }

        cc = result.getLocalised(DocFields.COUNTRY, language);
        if (cc != null) {
            var idx = todo.indexOf(" " + cc + " ");
            if (idx == 0 || idx + cc.length() + 2 >= todo.length()) {
                todo.delete(idx + 1, idx + cc.length() + 2);
                return cc.length();
            }
        }

        return 0.0;
    }

    private String normalize(String in) {
        return WORD_BREAK_PATTERN.matcher(in.toLowerCase()).replaceAll(" ").strip();
    }

    private List<String> secondaryNames(PhotonResult result) {
        var out = new ArrayList<String>();

        mapNames(result.getLocalised(DocFields.NAME, "default"), out);
        mapNames(result.getLocalised(DocFields.NAME, "alt"), out);

        return out;
    }

    private void mapNames(@Nullable String in, ArrayList<String> list) {
        if (in != null) {
            for (var s : in.split(";")) {
                var finname = normalize(s);
                if (!finname.isEmpty()) {
                    list.add(finname);
                }
            }
        }
    }

}

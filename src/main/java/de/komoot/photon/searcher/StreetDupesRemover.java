package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Filter out duplicate streets from the list.
 */
@NullMarked
public class StreetDupesRemover {
    private final String language;

    public StreetDupesRemover(String language) {
        this.language = language;
    }

    public List<PhotonResult> execute(List<PhotonResult> results) {
        final List<PhotonResult> filteredItems = new ArrayList<>(results.size());
        final HashSet<String> keys = new HashSet<>();

        for (PhotonResult result : results) {
            if ("highway".equals(result.get(DocFields.OSM_KEY))) {
                // result is a street
                final String postcode = (String) result.get(DocFields.POSTCODE);
                final String name = result.getLocalised(DocFields.NAME, language);

                if (postcode != null && name != null) {
                    // street has a postcode and name

                    // OSM_VALUE is part of key to avoid deduplication of e.g. bus_stops and streets with same name
                    String key = (String) result.get(DocFields.OSM_VALUE);
                    if (key == null) {
                        key = "";
                    }

                    if (language.equals("nl")) {
                        final String onlyDigitsPostcode = stripNonDigits(postcode);
                        key += ":" + onlyDigitsPostcode + ":" + name;
                    } else {
                        key += ":" + postcode + ":" + name;
                    }

                    if (keys.contains(key)) {
                        // an osm highway object (e.g. street or bus_stop) with this osm_value + name + postcode is already part of the result list
                        continue;
                    }
                    keys.add(key);
                }
            }
            filteredItems.add(result);
        }

        return filteredItems;
    }

    private static String stripNonDigits(
            final CharSequence input /* inspired by seh's comment */) {
        final StringBuilder sb = new StringBuilder(
                input.length() /* also inspired by seh's comment */);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c > 47 && c < 58) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}

package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
import org.jspecify.annotations.NullMarked;

import java.util.HashSet;
import java.util.function.Predicate;

/**
 * Filter out duplicate streets from the list.
 */
@NullMarked
public class StreetDupesRemover implements Predicate<PhotonResult> {
    final HashSet<String> keys = new HashSet<>();

    @Override
    public boolean test(PhotonResult result) {
        if ("highway".equals(result.get(DocFields.OSM_KEY))) {
            // result is a street
            final String name = result.getLocalised(DocFields.NAME, "default");

            if (name != null) {
                final String countryCode = (String) result.get(DocFields.COUNTRYCODE);
                String postcode = result.getOrDefault(DocFields.POSTCODE, "");

                final var pcLength = postcode.length();
                final var spacePos = postcode.indexOf(" ");
                if (spacePos > 0) {
                    if ("NL".equals(countryCode)) {
                        postcode = postcode.substring(0, spacePos);
                    } else if ("GB".equals(countryCode) && pcLength > spacePos + 2) {
                        postcode = postcode.substring(0, spacePos + 2);
                    }
                }

                // OSM_VALUE is part of key to avoid deduplication of e.g. bus_stops and streets with same name
                final String key = String.join(":",
                        result.getOrDefault(DocFields.OSM_VALUE, ""),
                        postcode,
                        name,
                        countryCode);

                return keys.add(key);
            }
        }

        return true;
    }
}

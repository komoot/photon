package de.komoot.photon.searcher;

import de.komoot.photon.Constants;
import de.komoot.photon.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Filter out duplicate streets from the list.
 */
public class StreetDupesRemover {
    private final String language;

    public StreetDupesRemover(String language) {
        this.language = language;
    }

    public List<PhotonResult> execute(List<PhotonResult> results) {
        final List<PhotonResult> filteredItems = new ArrayList<>(results.size());
        final HashSet<String> keys = new HashSet<>();

        for (PhotonResult result : results) {
            if ("highway".equals(result.get(Constants.OSM_KEY))) {
                // result is a street
                final String postcode = (String) result.get(Constants.POSTCODE);
                final String name = result.getLocalised(Constants.NAME, language);

                if (postcode != null && name != null) {
                    // street has a postcode and name

                    // OSM_VALUE is part of key to avoid deduplication of e.g. bus_stops and streets with same name
                    String key = (String) result.get(Constants.OSM_VALUE);
                    if (key == null) {
                        key = "";
                    }

                    if (language.equals("nl")) {
                        final String onlyDigitsPostcode = Utils.stripNonDigits(postcode);
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
}

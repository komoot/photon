package de.komoot.photon.searcher;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.komoot.photon.Constants;
import de.komoot.photon.Utils;
import de.komoot.photon.utils.Command;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;

/**
 * This is copy over from the method
 * <pre>private List<JSONObject> removeStreetDuplicates(List<JSONObject> results, String lang)</pre>
 * in class {@link de.komoot.photon.App}
 * <p/>
 * Created by Sachin Dole on 2/20/2015.
 */
public class StreetDupesRemover implements Command<List<JSONObject>, List<JSONObject>> {
    private final String language;

    public StreetDupesRemover(String language) {
        this.language = language;
    }

    @Override
    public List<JSONObject> execute(List<JSONObject>... allResults) {
        List<JSONObject> results = allResults[0];
        List<JSONObject> filteredItems = Lists.newArrayListWithCapacity(results.size());
        final HashSet<String> keys = Sets.newHashSet();
        for (JSONObject result : results) {
            final JSONObject properties = result.getJSONObject(Constants.PROPERTIES);
            if (properties.has(Constants.OSM_KEY) && "highway".equals(properties.getString(Constants.OSM_KEY))) {
                // result is a street
                if (properties.has(Constants.POSTCODE) && properties.has(Constants.NAME)) {
                    // street has a postcode and name
                    String postcode = properties.getString(Constants.POSTCODE);
                    String name = properties.getString(Constants.NAME);
                    String key;

                    if (language.equals("nl")) {
                        String onlyDigitsPostcode = Utils.stripNonDigits(postcode);
                        key = onlyDigitsPostcode + ":" + name;
                    } else {
                        key = postcode + ":" + name;
                    }

                    if (keys.contains(key)) {
                        // a street with this name + postcode is already part of the result list
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

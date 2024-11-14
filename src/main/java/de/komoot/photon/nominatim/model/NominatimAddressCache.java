package de.komoot.photon.nominatim.model;

import de.komoot.photon.nominatim.DBDataAdapter;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for caching information about address parts.
 */
public class NominatimAddressCache {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimAddressCache.class);

    private static final String BASE_COUNTRY_QUERY =
            "SELECT place_id, name, class, type, rank_address FROM placex" +
            " WHERE rank_address between 5 and 25 AND linked_place_id is null";

    private final Map<Long, AddressRow> addresses = new HashMap<>();

    public void loadCountryAddresses(JdbcTemplate template, DBDataAdapter dbutils, String countryCode) {
        final RowCallbackHandler rowMapper = rs ->
                addresses.put(
                        rs.getLong("place_id"),
                        new AddressRow(
                                Map.copyOf(dbutils.getMap(rs, "name")),
                                rs.getString("class"),
                                rs.getString("type"),
                                rs.getInt("rank_address")
                        ));


        if (countryCode == null) {
            template.query(BASE_COUNTRY_QUERY + " AND country_code is null", rowMapper);
        } else {
            template.query(BASE_COUNTRY_QUERY + " AND country_code = ?", rowMapper, countryCode);
        }

        if (addresses.size() > 0) {
            LOGGER.info("Loaded {} address places for country {}", addresses.size(), countryCode);
        }
    }

    public List<AddressRow> getAddressList(String addressline) {
        ArrayList<AddressRow> outlist = new ArrayList<>();

        if (addressline != null && !addressline.isBlank()) {
            JSONArray addressPlaces = new JSONArray(addressline);
            for (int i = 0; i < addressPlaces.length(); ++i) {
                Long placeId = addressPlaces.optLong(i);
                if (placeId != null) {
                    AddressRow row = addresses.get(placeId);
                    if (row != null) {
                        outlist.add(row);
                    }
                }
            }
        }

        return outlist;
    }
}

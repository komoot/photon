package de.komoot.photon.nominatim.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.nominatim.DBDataAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Container for caching information about address parts.
 */
public class NominatimAddressCache {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String BASE_COUNTRY_QUERY =
            "SELECT place_id, name, class, type, rank_address FROM placex" +
            " WHERE rank_address between 5 and 25 AND linked_place_id is null";

    private static final ObjectMapper objectMapper = new ObjectMapper();
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


        if ("".equals(countryCode)) {
            template.query(BASE_COUNTRY_QUERY + " AND country_code is null", rowMapper);
        } else {
            template.query(BASE_COUNTRY_QUERY + " AND country_code = ?", rowMapper, countryCode);
        }

        if (addresses.size() > 0) {
            LOGGER.info("Loaded {} address places for country {}", addresses.size(), countryCode);
        }
    }

    public List<AddressRow> getAddressList(String addressline) {
        if (addressline == null || addressline.isBlank()) {
            return new ArrayList<>();
        }

        Long[] placeIDs;
        try {
            placeIDs = objectMapper.readValue(addressline, Long[].class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot parse database response.", e);
            throw new RuntimeException("Parse error.");
        }

        return Arrays.stream(placeIDs)
                .map(id -> addresses.get(id))
                .filter(r -> r != null)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}

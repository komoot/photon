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
    private final RowCallbackHandler rowMapper;

    public NominatimAddressCache(DBDataAdapter dbutils, String[] languages) {
        rowMapper = rs -> {
            final var row = AddressRow.make(
                    dbutils.getMap(rs, "name"),
                    rs.getString("class"),
                    rs.getString("type"),
                    rs.getInt("rank_address"),
                    languages);
            if (!row.getName().isEmpty()) {
                addresses.put(
                        rs.getLong("place_id"), row);
            }
        };
    }

    public void loadCountryAddresses(JdbcTemplate template, String countryCode) {
        if ("".equals(countryCode)) {
            template.query(BASE_COUNTRY_QUERY + " AND country_code is null", rowMapper);
        } else {
            template.query(BASE_COUNTRY_QUERY + " AND country_code = ?", rowMapper, countryCode);
        }

        if (!addresses.isEmpty()) {
            LOGGER.info("Loaded {} address places for country {}", addresses.size(), countryCode);
        }
    }

    public List<AddressRow> getOrLoadAddressList(JdbcTemplate template, String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        final Long[] placeIDs = parsePlaceIdArray(json);

        final Long[] missing = Arrays.stream(placeIDs)
                .filter(id -> !addresses.containsKey(id)).toArray(Long[]::new);

        if (missing.length > 0) {
            template.query(
                    BASE_COUNTRY_QUERY + " AND place_id = ANY(?)",
                    rowMapper, (Object) missing);
        }

        return makeAddressList(placeIDs);
    }

    public List<AddressRow> getAddressList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        return makeAddressList(parsePlaceIdArray(json));
    }

    private List<AddressRow> makeAddressList(Long[] placeIDs) {
        return Arrays.stream(placeIDs)
                .map(addresses::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Long[] parsePlaceIdArray(String json) {
        try {
            return objectMapper.readValue(json, Long[].class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot parse database response.", e);
            throw new RuntimeException("Parse error.");
        }
    }
}

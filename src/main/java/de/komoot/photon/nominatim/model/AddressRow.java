package de.komoot.photon.nominatim.model;

import com.google.common.base.MoreObjects;
import lombok.Data;

import java.util.Arrays;
import java.util.Map;

/**
 * representation of an address as returned by nominatim's get_addressdata PL/pgSQL function
 *
 * @author christoph
 */
@Data
public class AddressRow {
    private static final String[] USEFUL_CONTEXT_KEYS = new String[]{"boundary", "landuse", "place"}; // must be in alphabetic order to speed up lookup
    private final long placeId;
    private final Map<String, String> name;
    private final String osmKey;
    private final String osmValue;
    private final int rankAddress;

    public AddressType getAddressType() {
        return AddressType.fromRank(rankAddress);
    }

    /**
     * @return whether nominatim thinks this place is a street
     */
    public boolean isStreet() {
        return AddressType.STREET.coversRank(rankAddress);
    }

    /**
     * @return whether nominatim thinks this place is a town or city
     */
    public boolean isCity() {
        return AddressType.CITY.coversRank(rankAddress);
    }

    /**
     * @return whether nominatim thinks this place is a state
     */
    public boolean isState() {
        return AddressType.STATE.coversRank(rankAddress);
    }

    public boolean isCountry() {
        return AddressType.COUNTRY.coversRank(rankAddress);
    }


    public boolean isPostcode() {
        if ("place".equals(osmKey) && "postcode".equals(osmValue)) {
            return true;
        }

        return "boundary".equals(osmKey) && "postal_code".equals(osmValue);
    }

    public boolean isUsefulForContext() {
        return !name.isEmpty() && !isPostcode() && Arrays.binarySearch(USEFUL_CONTEXT_KEYS, osmKey) >= 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("placeId", placeId)
                .add("name", name)
                .add("osmKey", osmKey)
                .add("osmValue", osmValue)
                .toString();
    }
}

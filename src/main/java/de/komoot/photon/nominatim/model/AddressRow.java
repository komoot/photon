package de.komoot.photon.nominatim.model;

import com.google.common.base.MoreObjects;
import lombok.Data;

import java.util.Arrays;
import java.util.Map;

import static de.komoot.photon.Constants.STATE;

/**
 * representation of an address as returned by nominatim's get_addressdata PL/pgSQL function
 *
 * @author christoph
 */
@Data
public class AddressRow {
    final private long placeId;
    final private Map<String, String> name;
    final private String osmKey;
    final private String osmValue;
    final private int rankAddress;
    final Integer adminLevel;
    final private String postcode;
    final private String place;
    final private String osmType;
    final private long osmId;

    static private final String[] USEFUL_CONTEXT_KEYS = new String[]{"boundary", "landuse", "place"}; // must be in alphabetic order to speed up lookup

     public boolean isStreet() {
        return 26 <= rankAddress && rankAddress < 28;
    }

    public boolean isLocality() {
        return 22 <= rankAddress && rankAddress < 26;
    }
    
    public boolean isDistrict() {
        return 17 <= rankAddress && rankAddress < 22;
    }
    public boolean isCity() {
        return 13 <= rankAddress && rankAddress <= 16;
    }

    public boolean isPostcode() {
        if ("place".equals(osmKey) && "postcode".equals(osmValue)) {
            return true;
        }

        if ("boundary".equals(osmKey) && "postal_code".equals(osmValue)) {
            return true;
        }

        return false;
    }

    public boolean hasPostcode() {
        return postcode != null; // TODO really null?
    }

    public boolean hasPlace() {
        return place != null;
    }

    public boolean isUsefulForContext() {
        if (name.isEmpty()) {
            return false;
        }

        if (isPostcode()) {
            return false;
        }

        if (rankAddress < 4) {
            // continent, sea, ...
            return false;
        }

        if (Arrays.binarySearch(USEFUL_CONTEXT_KEYS, osmKey) >= 0) {
            return true;
        }

        return false;
    }

    public boolean isCountry() {
        if (rankAddress == 4 && "boundary".equals(osmKey) && "administrative".equals(osmValue)) {
            return true;
        }


        return false;
    }

    public boolean isState() {
        return (5 <= rankAddress && rankAddress <= 9);
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

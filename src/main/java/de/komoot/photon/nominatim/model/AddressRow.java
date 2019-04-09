package de.komoot.photon.nominatim.model;

import static de.komoot.photon.Constants.STATE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.google.common.base.Objects;

import lombok.Data;

/**
 * representation of an address as returned by nominatim's get_addressdata PL/pgSQL function
 *
 * @author christoph
 */
@Data
public class AddressRow {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AddressRow.class);
    
    private static final String CURATED_CITIES = "curated_cities.txt";
    
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

    static private final String[] CITY_PLACE_VALUES = new String[]{"city", "hamlet", "town", "village"}; // must be in alphabetic order to speed up lookup
    static private final String[] USEFUL_CONTEXT_KEYS = new String[]{"boundary", "landuse", "place"}; // must be in alphabetic order to speed up lookup

    // relation ids that are known to be cities, but cannot be deduced by mapping as they are mapped in a special way
    // see https://github.com/komoot/photon/issues/138
    static private final long[] CURATED_CITY_RELATION_IDS;

    static {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        List<Long> idList = new ArrayList<>();
        try (InputStream is = loader.getResourceAsStream(CURATED_CITIES); Scanner scanner = new Scanner(is);) {
            if (is != null) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.startsWith("#")) {
                        try {
                            idList.add(Long.parseLong(line));
                        } catch (NumberFormatException nfe) {
                            LOGGER.error(String.format("Unable to parse %s %s",line, nfe.getMessage()));
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            LOGGER.error(String.format("Unable to read %s %s", CURATED_CITIES, ioe.getMessage()));
        }
        CURATED_CITY_RELATION_IDS = new long[idList.size()];
        for (int i = 0; i < idList.size(); i++) {
            CURATED_CITY_RELATION_IDS[i] = idList.get(i);
        }
        Arrays.sort(CURATED_CITY_RELATION_IDS);
        LOGGER.info(String.format("Read %d entries from %s", CURATED_CITY_RELATION_IDS.length, CURATED_CITIES));
    }

    public boolean isStreet() {
        return 26 <= rankAddress && rankAddress < 28;
    }

    public boolean isCity() {
        if ("place".equals(osmKey) && Arrays.binarySearch(CITY_PLACE_VALUES, osmValue) >= 0) {
            return true;
        }

        if (place != null && Arrays.binarySearch(CITY_PLACE_VALUES, place) >= 0) {
            return true;
        }

        if (adminLevel != null && adminLevel == 8 && "boundary".equals(osmKey) && "administrative".equals(osmValue)) {
            return true;
        }

        return false;
    }

    /**
     * whether address row was manually marked as city
     *
     * @return true if this row describes a city
     */
    public boolean isCuratedCity() {
        if ("R".equals(osmType) && Arrays.binarySearch(CURATED_CITY_RELATION_IDS, osmId) >= 0 ) {
            return true;
        }
        return false;
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

        if (isCuratedCity()) {
            // was already added to city
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
        if (adminLevel != null && adminLevel == 2 && "boundary".equals(osmKey) && "administrative".equals(osmValue)) {
            return true;
        }

        if ("place".equals(osmKey) && "country".equals(osmValue)) {
            return true;
        }

        return false;
    }

    public boolean isState() {
        if ("place".equals(osmKey) && STATE.equals(osmValue)) {
            return true;
        }

        if (adminLevel != null && adminLevel == 4 && "boundary".equals(osmKey) && "administrative".equals(osmValue)) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("placeId", placeId)
                .add("name", name)
                .add("osmKey", osmKey)
                .add("osmValue", osmValue)
                .toString();
    }
}

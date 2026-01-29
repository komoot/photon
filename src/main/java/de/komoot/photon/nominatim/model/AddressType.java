package de.komoot.photon.nominatim.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * List of address ranks available to Photon.
 * <p>
 * The different types correspond to the address parts available in GeocodeJSON. This type also defines
 * the mapping toward Nominatim's address ranks.
 */
@NullMarked
public enum AddressType {
    HOUSE("house", 29, 30, 3),
    STREET("street", 26, 28, 2),
    LOCALITY("locality", 22, 25, 1),
    DISTRICT("district", 17, 21, 1),
    CITY("city", 13, 16, 3),
    COUNTY("county", 10, 12, 1),
    STATE("state", 5, 9, 1),
    COUNTRY("country", 4, 4, 2),
    OTHER("other", 0, 0, 1);

    private final String name;
    private final int minRank;
    private final int maxRank;
    private final int searchPrio;

    AddressType(String name, int minRank, int maxRank, int searchPrio) {
        this.name = name;
        this.minRank = minRank;
        this.maxRank = maxRank;
        this.searchPrio = searchPrio;
    }

    /**
     * Convert a Nominatim address rank into a Photon address type.
     *
     * @param addressRank Nominatim address rank.
     * @return The corresponding address type or null if not covered.
     */
    @Nullable
    public static AddressType fromRank(int addressRank) {
        for (AddressType a : AddressType.values()) {
            if (a.coversRank(addressRank)) {
                return a;
            }
        }

        return null;
    }

    /**
     * Check if the given address rank is mapped to the given address type.
     *
     * @param addressRank Nominatim address rank.
     * @return True, if the type covers the rank.
     */
    public boolean coversRank(int addressRank) {
        return addressRank >= minRank && addressRank <= maxRank;
    }

    public String getName() {
        return name;
    }

    public static List<String> getNames() {
        return Arrays.stream(AddressType.values()).map(AddressType::getName).collect(Collectors.toList());
    }

    public int getSearchPrio() {
        return searchPrio;
    }
}

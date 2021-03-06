package de.komoot.photon.nominatim.model;

/**
 * List of address ranks available to Photon.
 * <p>
 * The different types correspond to the address parts available in GeocodeJSON. This type also defines
 * the mapping toward Nominatim's address ranks.
 */
public enum AddressType {
    HOUSE("house", 29, 30),
    STREET("street", 26, 28),
    LOCALITY("locality", 22, 26),
    DISTRICT("district", 17, 22),
    CITY("city", 13, 17),
    COUNTY("county", 10, 13),
    STATE("state", 5, 10),
    COUNTRY("country", 4, 4);

    private final String name;
    private final int minRank;
    private final int maxRank;

    AddressType(String name, int minRank, int maxRank) {
        this.name = name;
        this.minRank = minRank;
        this.maxRank = maxRank;
    }

    /**
     * Convert a Nominatim address rank into a Photon address type.
     *
     * @param addressRank Nominatim address rank.
     * @return The corresponding address type or null if not covered.
     */
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
}

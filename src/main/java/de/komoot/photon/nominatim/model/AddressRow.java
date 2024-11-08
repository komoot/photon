package de.komoot.photon.nominatim.model;

import java.util.Map;

/**
 * Representation of an address as returned by Nominatim's get_addressdata PL/pgSQL function.
 */
public class AddressRow {
    private final Map<String, String> name;
    private final String osmKey;
    private final String osmValue;
    private final int rankAddress;

    public AddressRow(Map<String, String> name, String osmKey, String osmValue, int rankAddress) {
        this.name = name;
        this.osmKey = osmKey;
        this.osmValue = osmValue;
        this.rankAddress = rankAddress;
    }

    public AddressType getAddressType() {
        return AddressType.fromRank(rankAddress);
    }

    private boolean isPostcode() {
        if ("place".equals(osmKey) && "postcode".equals(osmValue)) {
            return true;
        }

        return "boundary".equals(osmKey) && "postal_code".equals(osmValue);
    }

    public boolean isUsefulForContext() {
        return !name.isEmpty() && !isPostcode();
    }

    public Map<String, String> getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "AddressRow{" +
                "name=" + name.getOrDefault("name", "?") +
                ", osmKey='" + osmKey + '\'' +
                ", osmValue='" + osmValue + '\'' +
                ", rankAddress=" + rankAddress +
                '}';
    }
}

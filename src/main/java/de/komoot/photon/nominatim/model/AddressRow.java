package de.komoot.photon.nominatim.model;

import java.util.Map;

/**
 * Representation of an address as returned by Nominatim's get_addressdata PL/pgSQL function.
 */
public class AddressRow {
    private final Map<String, String> name;
    private final AddressType addressType;
    private final boolean isPostCode;

    private AddressRow(Map<String, String> name, AddressType addressType, boolean isPostCode) {
        this.name = name;
        this.addressType = addressType;
        this.isPostCode = isPostCode;
    }

    public static AddressRow make(Map<String, String> name, String osmKey, String osmValue, int rankAddress) {
        boolean isPostcode = ("place".equals(osmKey) && "postcode".equals(osmValue))
                || ("boundary".equals(osmKey) && "postal_code".equals(osmValue));
        return new AddressRow(name, AddressType.fromRank(rankAddress), isPostcode);
    }

    public AddressType getAddressType() {
        return addressType;
    }

    public boolean isPostcode() {
        return isPostCode;
    }

    public boolean isUsefulForContext() {
        return !name.isEmpty() && !isPostcode();
    }

    public Map<String, String> getName() {
        return this.name;
    }
}

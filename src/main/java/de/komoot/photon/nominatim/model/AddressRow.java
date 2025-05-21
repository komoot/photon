package de.komoot.photon.nominatim.model;

import java.util.Map;

/**
 * Representation of an address as returned by Nominatim's get_addressdata PL/pgSQL function.
 */
public class AddressRow {
    private final NameMap name;
    private final ContextMap context;
    private final AddressType addressType;
    private final boolean isPostCode;

    private AddressRow(NameMap name, ContextMap context, AddressType addressType, boolean isPostCode) {
        this.name = name;
        this.context = context;
        this.addressType = addressType;
        this.isPostCode = isPostCode;
    }

    public static AddressRow make(Map<String, String> name, String osmKey, String osmValue,
                                  int rankAddress, String[] languages) {
        ContextMap context = new ContextMap();

        if (("place".equals(osmKey) && "postcode".equals(osmValue))
                || ("boundary".equals(osmKey) && "postal_code".equals(osmValue))) {
            return new AddressRow(
                    new NameMap().setName("ref", name, "ref"),
                    context,
                    AddressType.fromRank(rankAddress),
                    true
            );
        }

        return new AddressRow(
                new NameMap().setLocaleNames(name, languages),
                context,
                AddressType.fromRank(rankAddress),
                false);
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

    public ContextMap getContext() {
        return context;
    }
}

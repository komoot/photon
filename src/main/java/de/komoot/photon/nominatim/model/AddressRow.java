package de.komoot.photon.nominatim.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Representation of an address as returned by Nominatim's get_addressdata PL/pgSQL function.
 */
@NullMarked
public class AddressRow {
    private final NameMap name;
    private final ContextMap context;
    @Nullable private final AddressType addressType;

    private AddressRow(NameMap name, ContextMap context, @Nullable AddressType addressType) {
        this.name = name;
        this.context = context;
        this.addressType = addressType;
    }

    public static AddressRow make(Map<String, String> name, String osmKey, String osmValue,
                                  AddressType addressType, Set<String> languages) {
        ContextMap context = new ContextMap();

        // Makes US state abbreviations searchable.
        context.addName("default", name.get("ISO3166-2"));

        return new AddressRow(
                new NameMap().setLocaleNames(name, languages),
                context,
                addressType);
    }

    @Nullable
    public AddressType getAddressType() {
        return addressType;
    }

    public NameMap getName() {
        return this.name;
    }

    public ContextMap getContext() {
        return context;
    }
}

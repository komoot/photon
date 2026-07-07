package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressType;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.regex.Pattern;

@NullMarked
public class PhotonDocAddressSet implements Iterable<PhotonDoc> {
    private static final Pattern HOUSENUMBER_CHECK = Pattern.compile("(\\A|.*,)[^\\d,]{3,}(,.*|\\Z)");
    private static final Pattern HOUSENUMBER_SPLIT = Pattern.compile("\\s*[;,]\\s*");

    private final List<PhotonDoc> docs = new ArrayList<>();

    public PhotonDocAddressSet(PhotonDoc base, Map<String, String> address) {
        this(base, address, false);
    }

    /**
     * Build the set of address documents for a place.
     *
     * @param base                  The document the addresses are derived from.
     * @param address               The address key/value pairs of the place.
     * @param streetHousenumberFull When {@code true}, street-based addresses that carry a
     *                              separate street number (i.e. the conscription/orientation
     *                              split used in countries such as Czechia and Slovakia) are
     *                              indexed with the full combined house number (the
     *                              {@code housenumber} key, e.g. {@code 2531/80}) instead of the
     *                              plain street number. The house number analyzer still splits
     *                              the combined form, so the street number and the conscription
     *                              number stay searchable on their own. When {@code false} (the
     *                              default) the plain street number is used.
     */
    public PhotonDocAddressSet(PhotonDoc base, Map<String, String> address, boolean streetHousenumberFull) {
        addPlaceAddress(base, address, "conscriptionnumber");
        addStreetAddress(base, address, streetHousenumberFull);

        if (docs.isEmpty()) {
            addGenericAddress(base, address, "housenumber");
        }

        if (docs.isEmpty() && base.isUsefulForIndex()) {
            // Doesn't have housenumbers, so add the original document as the single document
            docs.add(base);
        }
    }

    @Override
    public Iterator<PhotonDoc> iterator() {
        return docs.iterator();
    }

    private void addPlaceAddress(PhotonDoc base, Map<String, String> address, @SuppressWarnings("SameParameterValue") String key) {
        final String place = address.get("place");
        if (place != null && !place.isBlank()) {
            Map<AddressType, Map<String, String>> placeAddress = null;
            for (String hnr : splitHousenumber(address, key)) {
                if (placeAddress == null) {
                    placeAddress = copyAddressWithStreet(base, place);
                }

                docs.add(new PhotonDoc(base).replaceAddress(placeAddress).houseNumber(hnr));
            }
        }
    }

    private void addStreetAddress(PhotonDoc base, Map<String, String> address, boolean streetHousenumberFull) {
        if (!address.containsKey("street")) {
            return;
        }

        // Addresses that split the house number into a street (orientation) number and a
        // conscription number (e.g. in Czechia and Slovakia) also carry the two joined as a
        // combined house number such as "2531/80". When requested, index that combined form
        // instead of the plain street number. The house number analyzer splits it on the
        // delimiter, so the street number and the conscription number remain searchable on
        // their own. Ordinary addresses without a separate street number are left untouched.
        final String key = streetHousenumberFull && address.containsKey("streetnumber")
                ? "housenumber" : "streetnumber";

        for (String hnr : splitHousenumber(address, key)) {
            docs.add(new PhotonDoc(base).houseNumber(hnr));
        }
    }

    private void addGenericAddress(PhotonDoc base, Map<String, String> address, @SuppressWarnings("SameParameterValue") String key) {
        Map<AddressType, Map<String, String>> placeAddress = null;
        for (String hnr : splitHousenumber(address, key)) {
            if (placeAddress == null) {
                String block = address.get("block_number");
                if (block != null && !block.isBlank()) {
                    // block numbers replace streets unconditionally.
                    // We assume that addr:street is a tagging error and ignore it.
                    placeAddress = copyAddressWithStreet(base, block);
                } else {
                    String place = address.get("place");
                    // When addr:place and addr:Street appear together, then assume
                    // that addr:place is a tagging error and ignore it.
                    if (place != null && !place.isBlank() && !address.containsKey("street")) {
                        placeAddress = copyAddressWithStreet(base, place);
                    } else {
                        placeAddress = base.getAddressParts();
                    }
                }
            }

            docs.add(new PhotonDoc(base).replaceAddress(placeAddress).houseNumber(hnr));
        }
    }

    private String[] splitHousenumber(Map<String, String> address, String key) {
        final String value = address.get(key);

        if (value == null || value.isBlank() || HOUSENUMBER_CHECK.matcher(value).find()) {
            return new String[]{};
        }

        return Arrays.stream(HOUSENUMBER_SPLIT.split(value))
                .filter(s -> !s.isBlank() && s.length() < 20)
                .map(String::trim)
                .toArray(String[]::new);
    }

    private EnumMap<AddressType, Map<String, String>> copyAddressWithStreet(PhotonDoc base, String streetName) {
        final EnumMap<AddressType, Map<String, String>> outmap = new EnumMap<>(AddressType.class);

        for (var entry : base.getAddressParts().entrySet()) {
            if (entry.getKey() != AddressType.STREET) {
                outmap.put(entry.getKey(), entry.getValue());
            }
        }

        outmap.put(AddressType.STREET, Map.of("default", streetName));

        return outmap;
    }

}

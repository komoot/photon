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
        addPlaceAddress(base, address, "conscriptionnumber");
        addStreetAddress(base, address, "streetnumber");

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

    private void addStreetAddress(PhotonDoc base, Map<String, String> address, @SuppressWarnings("SameParameterValue") String key) {
        if (address.containsKey("street")) {
            for (String hnr : splitHousenumber(address, key)) {
                docs.add(new PhotonDoc(base).houseNumber(hnr));
            }
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

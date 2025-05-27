package de.komoot.photon;

import de.komoot.photon.nominatim.model.AddressType;

import java.util.*;
import java.util.regex.Pattern;

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

    private void addPlaceAddress(PhotonDoc base, Map<String, String> address, String key) {
        Map<AddressType, Map<String, String>> placeAddress = null;
        for (String hnr : splitHousenumber(address, key)) {
            if (placeAddress == null) {
                placeAddress = new EnumMap<>(AddressType.class);

                for (var entry : base.getAddressParts().entrySet()) {
                    if (entry.getKey() != AddressType.STREET) {
                        placeAddress.put(entry.getKey(), entry.getValue());
                    }
                }

                final String place = address.get("place");
                if (place != null && !place.isBlank()) {
                    placeAddress.put(AddressType.STREET, Map.of("default", place));
                }
            }

            docs.add(new PhotonDoc(base).replaceAddress(placeAddress).houseNumber(hnr));
        }
    }

    private void addStreetAddress(PhotonDoc base, Map<String, String> address, String key) {
        for (String hnr : splitHousenumber(address, key)) {
            docs.add(new PhotonDoc(base).houseNumber(hnr));
        }
    }

    private void addGenericAddress(PhotonDoc base, Map<String, String> address, String key) {
        Map<AddressType, Map<String, String>> placeAddress = null;
        for (String hnr : splitHousenumber(address, key)) {
            if (placeAddress == null) {
                String place = address.get("place");
                if (place == null || place.isBlank()) {
                    place = address.get("block_number");
                }
                if (place != null && !place.isBlank()) {
                    placeAddress = new EnumMap<>(AddressType.class);

                    for (var entry : base.getAddressParts().entrySet()) {
                        if (entry.getKey() != AddressType.STREET) {
                            placeAddress.put(entry.getKey(), entry.getValue());
                        }
                    }

                    placeAddress.put(AddressType.STREET, Map.of("default", place));
                } else {
                    placeAddress = base.getAddressParts();
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

}

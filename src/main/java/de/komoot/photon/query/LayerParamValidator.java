package de.komoot.photon.query;

import de.komoot.photon.nominatim.model.AddressType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LayerParamValidator {
    private static final List<String> availableLayers = AddressType.getNames();

    public Set<String> validate(String[] filters) throws BadRequestException {
        Set<String> resultFilter = new HashSet<>();

        for (String layerFilter : filters) {
            if (!availableLayers.contains(layerFilter)) {
                throw new BadRequestException(
                    400,
                    String.format("Invalid layer '%s'. Allowed layers are: %s", layerFilter, String.join(",", availableLayers))
                );
            }

            resultFilter.add(layerFilter);
        }

        return resultFilter;
    }
}

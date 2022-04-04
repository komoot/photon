package de.komoot.photon.query;

import de.komoot.photon.nominatim.model.AddressType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObjectTypeParamValidator {
    private static final List<String> availableTypes = AddressType.getNames();

    public Set<String> validate(String[] filters) throws BadRequestException {
        Set<String> resultFilter = new HashSet<>();

        for (String objectTypeFilter : filters) {
            if (!availableTypes.contains(objectTypeFilter)) {
                throw new BadRequestException(
                    400,
                    String.format("Invalid object_type '%s'. Allowed types are: %s", objectTypeFilter, String.join(",", availableTypes))
                );
            }

            resultFilter.add(objectTypeFilter);
        }

        return resultFilter;
    }
}

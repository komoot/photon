package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class NominatimDumpFileFeatures {

    @JsonProperty("sorted_by_country")
    public boolean isSortedByCountry = true;

    @JsonProperty("has_addresslines")
    public boolean hasAddressLines = false;
}

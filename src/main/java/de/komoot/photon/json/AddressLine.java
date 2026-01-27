package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class AddressLine {

    @JsonProperty("place_id")
    @Nullable public String placeId;

    @JsonProperty("rank_address")
    public int rankAddress;

    @JsonProperty("isaddress")
    public boolean isAddress;
}

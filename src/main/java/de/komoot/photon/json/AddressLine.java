package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddressLine {

    @JsonProperty("place_id")
    public String placeId;

    @JsonProperty("rank_address")
    public int rankAddress;

    @JsonProperty("isaddress")
    public boolean isAddress;
}

package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.NominatimResult;

import java.util.Collections;
import java.util.Map;

public class NominatimPlaceDocument {
    public static final String DOCUMENT_TYPE = "place";

    private PhotonDoc doc = new PhotonDoc();
    private Map<String, String> address = Collections.emptyMap();

    public NominatimResult asNominatimResult() {
        return NominatimResult.fromAddress(doc, address);
    }

    @JsonProperty("place_id")
    void setPlaceId(long placeId) {
        doc.placeId(placeId)
    }
}

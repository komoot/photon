package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.PhotonDocAddressSet;

import java.util.Collections;
import java.util.Map;

public class NominatimPlaceDocument {
    public static final String DOCUMENT_TYPE = "place";

    private PhotonDoc doc = new PhotonDoc();
    private Map<String, String> address = Collections.emptyMap();

    public PhotonDoc asSimpleDoc() {
        return doc; // TODO do we need to merge address at this point?
    }

    public Iterable<PhotonDoc> asMultiAddressDocs() {
        return new PhotonDocAddressSet(doc, address);
    }

    @JsonProperty("place_id")
    void setPlaceId(long placeId) {
        doc.placeId(placeId);
    }
}

package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonRequest;
import org.json.JSONObject;

import java.util.List;

public interface SearchHandler {

    List<JSONObject> search(PhotonRequest photonRequest);

    String dumpQuery(PhotonRequest photonRequest);
}

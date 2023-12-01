package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonRequest;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public interface SearchHandler {

    List<PhotonResult> search(PhotonRequest photonRequest) throws IOException;

    String dumpQuery(PhotonRequest photonRequest);
}

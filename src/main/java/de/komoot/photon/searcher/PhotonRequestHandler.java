package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonRequest;
import org.json.JSONObject;

import java.util.List;

/**
 * Given a {@link PhotonRequest photon request}, execute the search, process it (for example, de-duplicate) and respond with results formatted in a list of {@link JSONObject json
 * object}s.
 * <p/>
 * Created by Sachin Dole on 2/12/2015.
 */
public interface PhotonRequestHandler<R extends PhotonRequest> {
    List<JSONObject> handle(R photonRequest);
}

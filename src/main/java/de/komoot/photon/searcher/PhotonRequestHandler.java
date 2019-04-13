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
    /**
     * Use the request to query ES
     * 
     * @param photonRequest the request
     * @return a List of returned results
     */
    List<JSONObject> handle(R photonRequest);
    
    /**
     * Get a JSON representation of the query that would be built from the request
     * 
     * @param photonRequest the request
     * @return a String containing JSON
     */
    String dumpQuery(R photonRequest);
}

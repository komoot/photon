package de.komoot.photon.searcher;

import de.komoot.photon.query.ReverseRequest;
import org.json.JSONObject;
import java.util.List;

/**
 *
 * @author svantulden
 */
public interface ReverseRequestHandler <R extends ReverseRequest> {
    List<JSONObject> handle(R reverseRequest);
}

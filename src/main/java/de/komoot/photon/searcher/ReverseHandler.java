package de.komoot.photon.searcher;

import de.komoot.photon.query.ReverseRequest;
import org.json.JSONObject;

import java.util.List;

public interface ReverseHandler {

    List<JSONObject> reverse(ReverseRequest photonRequest);
}

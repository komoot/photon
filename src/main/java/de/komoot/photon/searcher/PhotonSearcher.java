package de.komoot.photon.searcher;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by sachi_000 on 2/12/2015.
 */
public interface PhotonSearcher {
    List<JSONObject> search();

    List<JSONObject> searchStrict();
}

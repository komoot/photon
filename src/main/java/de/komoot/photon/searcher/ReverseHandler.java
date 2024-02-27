package de.komoot.photon.searcher;

import de.komoot.photon.query.ReverseRequest;

import java.util.List;

/**
 * Interface for a handler of reverse geocoding requests.
 */
public interface ReverseHandler {

    List<PhotonResult> reverse(ReverseRequest photonRequest);

    String dumpQuery(ReverseRequest photonRequest);
}

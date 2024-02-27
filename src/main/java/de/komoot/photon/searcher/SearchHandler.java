package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonRequest;

import java.util.List;

/**
 * Interface for a handler of search geocoding requests.
 */
public interface SearchHandler {

    List<PhotonResult> search(PhotonRequest photonRequest);

    String dumpQuery(PhotonRequest photonRequest);
}

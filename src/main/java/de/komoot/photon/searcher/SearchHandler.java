package de.komoot.photon.searcher;

import de.komoot.photon.query.SimpleSearchRequest;

import java.util.List;

/**
 * Interface for a handler of search geocoding requests.
 */
public interface SearchHandler {

    List<PhotonResult> search(SimpleSearchRequest simpleSearchRequest);

    String dumpQuery(SimpleSearchRequest simpleSearchRequest);
}

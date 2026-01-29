package de.komoot.photon.searcher;

import de.komoot.photon.query.RequestBase;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Interface for a handler of search geocoding requests.
 */
@NullMarked
public interface SearchHandler<T extends RequestBase> {

    List<PhotonResult> search(T searchRequest);

    String dumpQuery(T searchRequest);
}

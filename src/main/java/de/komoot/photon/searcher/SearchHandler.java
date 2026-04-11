package de.komoot.photon.searcher;

import de.komoot.photon.query.RequestBase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Interface for a handler of search geocoding requests.
 */
@NullMarked
public interface SearchHandler<T extends RequestBase> {

    Stream<PhotonResult> search(T searchRequest);

    @Nullable String dumpQuery(T searchRequest);
}

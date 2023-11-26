package de.komoot.photon.searcher;

import de.komoot.photon.query.LookupRequest;

import java.util.List;

public interface LookupHandler {
    List<PhotonResult> lookup(LookupRequest lookupRequest);
}

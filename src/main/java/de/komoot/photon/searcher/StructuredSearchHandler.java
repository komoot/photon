package de.komoot.photon.searcher;

import de.komoot.photon.query.StructuredSearchRequest;

import java.util.List;

public interface StructuredSearchHandler {
    List<PhotonResult> search(StructuredSearchRequest photonRequest);
}

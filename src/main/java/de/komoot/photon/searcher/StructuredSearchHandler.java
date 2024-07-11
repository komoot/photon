package de.komoot.photon.searcher;

import de.komoot.photon.query.StructuredPhotonRequest;

import java.util.List;

public interface StructuredSearchHandler {
    List<PhotonResult> search(StructuredPhotonRequest photonRequest);
}

package de.komoot.photon.searcher;

import de.komoot.photon.query.ReverseRequest;

import java.io.IOException;
import java.util.List;

public interface ReverseHandler {

    List<PhotonResult> reverse(ReverseRequest photonRequest) throws IOException;

    String dumpQuery(ReverseRequest photonRequest);
}

package de.komoot.photon.searcher;

import java.io.IOException;

public interface LookupHandler {
    PhotonResult lookup(String lookupRequest) throws IOException;
}

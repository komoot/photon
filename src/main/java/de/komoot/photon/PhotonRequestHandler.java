package de.komoot.photon;

import de.komoot.photon.query.PhotonRequest;

/**
 * Created by sachi_000 on 2/12/2015.
 */
public interface PhotonRequestHandler {
    String handle(PhotonRequest photonRequest);
}

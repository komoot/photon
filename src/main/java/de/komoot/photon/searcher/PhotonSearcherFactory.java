package de.komoot.photon.searcher;

import de.komoot.photon.query.FilteredPhotonRequest;
import de.komoot.photon.query.PhotonRequest;

/**
 * Created by sachi_000 on 2/12/2015.
 */
public class PhotonSearcherFactory {

    public PhotonSearcher getSearcher(PhotonRequest photonRequest) {
        if (photonRequest instanceof FilteredPhotonRequest) {
            return new BasePhotonSearcher();
        } else return null;

    }

}

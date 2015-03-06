package de.komoot.photon.searcher;

import de.komoot.photon.query.FilteredPhotonRequest;
import de.komoot.photon.query.PhotonRequest;

/**
 * Create a {@link ElasticsearchSearcher elastic search searcher} that will handle all specifics related to elastic search.
 * <p/>
 * Created by sachi_000 on 2/12/2015.
 */
public class ElasticsearchSearcherFactory {

    public ElasticsearchSearcher getSearcher(PhotonRequest photonRequest) {
        if (photonRequest instanceof FilteredPhotonRequest) {
            return new BaseElasticsearchSearcher();
        } else return null;

    }

}

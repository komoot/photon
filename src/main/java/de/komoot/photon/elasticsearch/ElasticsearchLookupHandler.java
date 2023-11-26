package de.komoot.photon.elasticsearch;

import de.komoot.photon.query.LookupRequest;
import de.komoot.photon.searcher.LookupHandler;
import de.komoot.photon.searcher.PhotonResult;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import java.util.ArrayList;
import java.util.List;


public class ElasticsearchLookupHandler implements LookupHandler {
    private final Client client;

    public ElasticsearchLookupHandler(Client client) {
        this.client = client;
    }

    public List<PhotonResult> lookup(LookupRequest request) {
        GetRequestBuilder builder = this.client.prepareGet().
                setIndex("photon").
                setType("place").
                setId(request.getPlaceId());
        GetResponse response = builder.execute().actionGet();
        List<PhotonResult> ret = new ArrayList<>(1);
        if (response != null) {
            ret.add(new ElasticResult(response));
        }
        return ret;
    }
}

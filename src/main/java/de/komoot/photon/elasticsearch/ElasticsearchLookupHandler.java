package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.searcher.LookupHandler;
import de.komoot.photon.searcher.PhotonResult;

import java.io.IOException;


public class ElasticsearchLookupHandler implements LookupHandler {
    private final ElasticsearchClient client;

    public ElasticsearchLookupHandler(ElasticsearchClient client) {
        this.client = client;
    }

    public PhotonResult lookup(String placeId) throws IOException {
        GetRequest request = new GetRequest.Builder().index(PhotonIndex.NAME).id(placeId).build();
        GetResponse<ObjectNode> response = client.get(request, ObjectNode.class);
        if (!response.found()) {
            return null;
        }
        return new ElasticResult(response.source());
    }
}

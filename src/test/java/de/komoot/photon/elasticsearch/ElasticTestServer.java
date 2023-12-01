package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.core.GetResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.searcher.PhotonResult;

import java.io.IOException;

public class ElasticTestServer extends Server {
    public ElasticTestServer(String mainDirectory) {
        super(mainDirectory);
    }

    @Override
    protected IndexSettings loadIndexSettings() {
        return new IndexSettings().setShards(1);
    }

    public PhotonResult getById(int id) throws IOException {
         GetResponse<ObjectNode> response = client.get(fn -> fn
                 .index(PhotonIndex.NAME)
                 .id(String.valueOf(id)),
                 ObjectNode.class
         );

        return response.found() ? new ElasticGetIdResult(response.source()) : null;
    }

    public void refresh() throws IOException {
        client.indices().refresh(fn -> fn.index(PhotonIndex.NAME));
    }
}

package de.komoot.photon;

import de.komoot.photon.elasticsearch.*;
import de.komoot.photon.searcher.PhotonResult;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetResponse;

public class ElasticTestServer extends Server {
    public ElasticTestServer(String mainDirectory) {
        super(mainDirectory);
    }

    @Override
    protected IndexSettings loadIndexSettings() {
        return new IndexSettings().setShards(1);
    }

    public PhotonResult getById(String id) {
        GetResponse response =  esClient.prepareGet(PhotonIndex.NAME,PhotonIndex.TYPE, id).execute().actionGet();

        return response.isExists() ? new ElasticGetIdResult(response) : null;
    }

    public void refresh() {
        esClient.admin().indices().refresh(new RefreshRequest(PhotonIndex.NAME)).actionGet();
    }
}

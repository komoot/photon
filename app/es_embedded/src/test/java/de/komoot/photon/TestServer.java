package de.komoot.photon;

import de.komoot.photon.elasticsearch.*;
import de.komoot.photon.searcher.PhotonResult;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetResponse;
import java.io.IOException;

public class TestServer extends Server {
    public static final String TEST_CLUSTER_NAME = "photon-test";

    public TestServer(String mainDirectory) {
        super(mainDirectory);
    }

    @Override
    protected IndexSettings loadIndexSettings() {
        return new IndexSettings().setShards(1);
    }

    public PhotonResult getByID(String id) {
        GetResponse response =  esClient.prepareGet(PhotonIndex.NAME,PhotonIndex.TYPE, id).execute().actionGet();

        return response.isExists() ? new ElasticGetIdResult(response) : null;
    }

    public void refresh() {
        esClient.admin().indices().refresh(new RefreshRequest(PhotonIndex.NAME)).actionGet();
    }

    public void startTestServer(String clusterName) {
        try {
            start(TEST_CLUSTER_NAME, new String[]{}, true);
        } catch (IOException e) {
            throw new RuntimeException("Setup error", e);
        }
    }

    public void stopTestServer() {
        shutdown();
    }

    public void refreshTestServer() {
            refreshIndexes();
    }

}

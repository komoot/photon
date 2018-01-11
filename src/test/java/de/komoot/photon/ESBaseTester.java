package de.komoot.photon;

import de.komoot.photon.elasticsearch.Server;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.junit.After;

import java.io.File;

/**
 * @author Peter Karich
 */
@Slf4j
public class ESBaseTester {
    private Server server;

    @After
    public void tearDownClass() {
        shutdownES();
    }

    public void setUpES() throws Exception {
        server = new Server("photon_test", new File("./target/es_photon").getAbsolutePath(), "en", "", true).start();
        server.recreateIndex();
        refresh();
    }

    protected Client getClient() {
        if (server == null) {
            throw new RuntimeException("call setUpES before using getClient");
        }

        return server.getClient();
    }

    private final String indexName = "photon";

    protected void refresh() {
        getClient().admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
    }

    public void shutdownES() {
        server.shutdown();
    }

    public void deleteIndex() {
        server.deleteIndex();
    }
}

package de.komoot.photon;

import de.komoot.photon.importer.elasticsearch.Server;
import java.io.File;
import java.io.IOException;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.AfterClass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Peter Karich
 */
public class ESBaseTester {

    protected static Logger logger = LoggerFactory.getLogger(ESBaseTester.class);
    protected static int jettyPort;
    protected int resolved;
    private static Server server;

    @AfterClass
    public static void tearDownClass() {
        shutdownES();
    }    

    public void setUpES() throws IOException {
        if (server != null)
            return;
        
        server = new Server("photon", new File("./target/es_photon").getAbsolutePath(), "en").start(true);
        server.recreateIndex();
    }

    protected Server getApiServer() {
        return server;
    }

    protected Client getClient() {
        if (server == null)
            throw new RuntimeException("call setUpES before using getClient");

        return server.getClient();
    }
    
    private final String indexName = "photon";

    protected void refresh() {
        getClient().admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
    }

    protected void deleteAll() {
        try {
            getClient().prepareDeleteByQuery(indexName).
                    setQuery(QueryBuilders.matchAllQuery()).
                    execute().actionGet();
        } catch (IndexMissingException ex) {
        }

        refresh();
    }
    public static void shutdownES() {
        if (server != null)
            server.shutdown();
    }
}

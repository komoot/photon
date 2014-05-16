package de.komoot.photon.importer.elasticsearch;

import org.elasticsearch.node.Node;
import org.elasticsearch.client.Client;
import static org.elasticsearch.node.NodeBuilder.*;

/**
 * Helper class to start/stop elasticserach node and get elasticsearch clients
 * @author felix
 */
public class Server {


    private Node esNode;
    private String clusterName;

    /**
     * starts the elasticsearch node
     */
    public void start(){
        this.esNode = nodeBuilder().clusterName(this.clusterName).loadConfigSettings(true).node();
    }


    /**
     * stops the elasticsearch node
     */
    public void shutdown(){
        this.esNode.close();
    }


    /**
     * returns an elasticsearch client
     */
    public Client getClient(){
        return this.esNode.client();
    }


    /**
     * @param clusterName name of the elasticsearch cluster
     */
    public Server(String clusterName){
        this.clusterName = clusterName;
    }
}

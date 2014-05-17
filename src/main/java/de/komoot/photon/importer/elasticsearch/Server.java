package de.komoot.photon.importer.elasticsearch;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.node.Node;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.action.ActionListener;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.*;
import net.lingala.zip4j.model.*;
import net.lingala.zip4j.util.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.io.File;
import java.net.URL;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;


import javax.xml.ws.Response;

import static org.elasticsearch.node.NodeBuilder.*;

/**
 * Helper class to start/stop elasticserach node and get elasticsearch clients
 * @author felix
 */
public class Server {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Importer.class);

    private Node esNode;
    private String clusterName;
    private File mainDirectory;
    private File esDirectory;
    private File dumpDirectory;
    private File updateDirectory;

    /**
     * starts the elasticsearch node
     */
    public void start(){
        ImmutableSettings.Builder settings =
                ImmutableSettings.settingsBuilder();
        settings.put("path.home", this.esDirectory.toString());

        this.esNode = nodeBuilder().clusterName(this.clusterName).loadConfigSettings(true).settings(settings).node();


    }


    /**
     * stops the elasticsearch node
     */
    public void shutdown(){
        this.esNode.close();
    }


    public void dump(String dumpName){

        LOGGER.warn("Create dump '"+dumpName+"' in "+this.dumpDirectory.getAbsolutePath());
        final File dumpLocation =  new File(this.dumpDirectory, dumpName);
        dumpLocation.mkdir();
        this.esNode.client().admin().cluster().putRepository(this.getClient().admin().cluster().preparePutRepository(dumpName)
                .setSettings(ImmutableSettings.settingsBuilder().put("compress", "true")
                .put("location", dumpLocation.getAbsolutePath())).setType("fs").request()).actionGet();

        this.getClient().admin().cluster().createSnapshot(this.getClient().admin().cluster().
                prepareCreateSnapshot(dumpName, dumpName).setIndices("photon").request(), new ActionListener<CreateSnapshotResponse>() {
            @Override
            public void onResponse(CreateSnapshotResponse createSnapshotResponse) {
                try {
                    ZipFile zipFile = new ZipFile(dumpLocation + ".zip");
                    ZipParameters parameters = new ZipParameters();
                    parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
                    parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
                    zipFile.addFolder(dumpLocation, parameters);
                    dumpLocation.delete();
                    LOGGER.warn("Created dump "+dumpLocation + ".zip");
                } catch (ZipException e) {

                }
            }

            @Override
            public void onFailure(Throwable e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

    }


    /**
     * returns an elasticsearch client
     */
    public Client getClient(){
        return this.esNode.client();
    }



    private File setupDirectories(URL directoryName){

        File mainDirectory = new File(".");

        try{
           mainDirectory = new File(directoryName.toURI());
        }
        catch (URISyntaxException e){
            LOGGER.error("Can´t access photon_data directory");
        }


        File photonDirectory = new File(mainDirectory, "photon_data");

        if(!photonDirectory.exists()){
            photonDirectory.mkdirs();
        }

        this.esDirectory = new File(photonDirectory, "elasticsearch");
        if(!this.esDirectory.exists())
        {
            this.esDirectory.mkdirs();
        }

        this.dumpDirectory = new File(photonDirectory, "dumps");
        if(!this.dumpDirectory.exists())
        {
            this.dumpDirectory.mkdirs();
        }

        this.updateDirectory = new File(photonDirectory, "updates");
        if(!this.updateDirectory.exists())
        {
            this.updateDirectory.mkdirs();
        }


        return mainDirectory;
    }
    /**
     * @param clusterName name of the elasticsearch cluster
     */
    public Server(String clusterName, String mainDirectory){

        try{
        this.mainDirectory = setupDirectories(new URL("file://"+mainDirectory));
        } catch (MalformedURLException e)
        {
            LOGGER.error("Can´t create directories");
        }
        this.clusterName = "defdewf";
    }
}

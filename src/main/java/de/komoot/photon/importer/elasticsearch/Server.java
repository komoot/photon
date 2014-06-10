package de.komoot.photon.importer.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.PluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;

import org.elasticsearch.node.NodeBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import org.elasticsearch.repositories.RepositoryMissingException;

/**
 * Helper class to start/stop elasticserach node and get elasticsearch clients
 *
 * @author felix
 */
@Slf4j
public class Server {
    
	private Node esNode;
	private static final String clusterName = "photon";        
        private File esDirectory;        
	private File dumpDirectory;
	private File updateDirectory;
	private File tempDirectory;
	private File importDirectory;
        
	public Server(String mainDirectory) {
            try {
		setupDirectories(new URL("file://" + mainDirectory));
            } catch(MalformedURLException e) {
			log.error("Can´t create directories");
            }
        }
                        
        public Server start() {
		return start(false);
        }
        
	public Server start(boolean test) {        
		ImmutableSettings.Builder sBuilder = ImmutableSettings.settingsBuilder()
				.put("path.home", this.esDirectory.toString());
                
                // default is 'local', 'none' means no data after node restart!
            
                if(test)
                    sBuilder.put("gateway.type", "none");
                    
		Settings settings = sBuilder.build();

		final String pluginPath = this.getClass().getResource("/elasticsearch-wordending-tokenfilter-0.0.1.zip").toExternalForm();
		PluginManager pluginManager = new PluginManager(new Environment(settings), pluginPath, PluginManager.OutputMode.VERBOSE, new TimeValue(30000));
		try {
			pluginManager.downloadAndExtract("ybon/elasticsearch-wordending-tokenfilter/0.0.1");
		} catch(IOException e) {
			log.debug("could not install ybon/elasticsearch-wordending-tokenfilter/0.0.1", e);
		}

                if(!test) {
                    pluginManager = new PluginManager(new Environment(settings), null, PluginManager.OutputMode.VERBOSE, new TimeValue(30000));
                    for(String pluginName : new String[]{"mobz/elasticsearch-head", "polyfractal/elasticsearch-inquisitor", "elasticsearch/marvel/latest"}) {
                            try {
                                    pluginManager.downloadAndExtract(pluginName);
                            } catch(IOException e) {
                            }
                    }
                }

		NodeBuilder nBuilder = nodeBuilder().clusterName(Server.clusterName).loadConfigSettings(true).
                        settings(settings);
                
                // problem with node ending plugin
//                if(test)
//                    nBuilder.local(true);
                
                esNode = nBuilder.node();
		log.info("started elastic search node");
                return this;
	}

	/**
	 * stops the elasticsearch node
	 */
	public void shutdown() {
		this.esNode.close();
	}

        public void createSnapshot(String dumpName) {
            log.info(String.format("Create snapshot '%s' in %s", dumpName, this.dumpDirectory.getAbsolutePath()));

            final File dumpLocation = new File(this.dumpDirectory, dumpName);
            dumpLocation.mkdir();
            PutRepositoryRequest putRepoRequest = this.getClient().admin().cluster().preparePutRepository(dumpName)
                    .setSettings(ImmutableSettings.settingsBuilder()
                            .put("location", dumpLocation.getAbsolutePath()))
                    .setType("fs").request();
            this.esNode.client().admin().cluster().putRepository(putRepoRequest).actionGet();

            try {
                String repoName = dumpName;
                CreateSnapshotRequest snapRequest = this.getClient().admin().cluster().
                        prepareCreateSnapshot(repoName, dumpName).
                        setIndices("photon").
                        setIncludeGlobalState(true).
                        setWaitForCompletion(true).
                        request();
                this.getClient().admin().cluster().createSnapshot(snapRequest).actionGet();
                ZipFile zipFile = new ZipFile(dumpLocation + ".zip");
                ZipParameters parameters = new ZipParameters();
                parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
                parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
                zipFile.addFolder(dumpLocation, parameters);
                
                removeDir(dumpLocation);

                log.info(String.format("Created snapshot: %s.zip", dumpLocation));
            } catch (ZipException e) {
                throw new RuntimeException("error creating zip file of snapshot", e);
            }
        }

        public File getDumpDirectory() {
            return dumpDirectory;
        }

        public static boolean removeDir( File file )
        {
            if (!file.exists())            
                return true;            

            if (file.isDirectory())            
                for (File f : file.listFiles())
                {
                    removeDir(f);
                }            

            return file.delete();
        }
        
        /**
         * Restores snapshot from specified dumpUrl.
         */
        public void importSnapshot(String dumpUrl, String dumpName) {
            File tempZip = new File(this.tempDirectory, dumpName + ".zip");
            try {
                // hmmh, if no shared file system we need to replicate it to other servers too
                FileUtils.copyFile(new File(new URL(dumpUrl).toURI()), tempZip);
            } catch (Exception e) {
                throw new RuntimeException("Error while loading dump. Is dumpUrl " + dumpUrl + " and dumpName " + dumpName + " correct?", e);
            }

            try {                
                ZipFile dumpZip = new ZipFile(tempZip);
                String dumpDir = this.dumpDirectory.getAbsolutePath();
                dumpZip.extractAll(dumpDir);
            } catch (ZipException e) {
                throw new RuntimeException(e);
            }            
            
            // repository can only load snapshot from configured dumpDir
            String repoName = dumpName;
            this.getClient().admin().cluster().
                    prepareRestoreSnapshot(repoName, dumpName).
                    setIndices("photon").
                    setWaitForCompletion(true).
                    get();
            log.info(String.format("imported snapshot "  + dumpName + " from " + getDumpDirectory()));
        }

        public void deleteSnapshot(String dumpName) {
            try {
                String repoName = dumpName;
                getClient().admin().cluster().deleteSnapshot(
                        new DeleteSnapshotRequest(repoName, dumpName)).actionGet();
            } catch(RepositoryMissingException ex) {
                // ignore
            }
        }
	/**
	 * returns an elasticsearch client
	 */
	public Client getClient() {
		return this.esNode.client();
	}

	private File setupDirectories(URL directoryName) {
		File mainDirectory = new File(".");

		try {
			mainDirectory = new File(directoryName.toURI());
		} catch(URISyntaxException e) {
			log.error("Can´t access photon_data directory");
		}

		File photonDirectory = new File(mainDirectory, "photon_data");
		this.esDirectory = new File(photonDirectory, "elasticsearch");
		this.dumpDirectory = new File(photonDirectory, "dumps");
		this.updateDirectory = new File(photonDirectory, "updates");
		this.tempDirectory = new File(photonDirectory, "temp");
		this.importDirectory = new File(photonDirectory, "imports");

		for(File directory : new File[]{esDirectory, dumpDirectory, updateDirectory, importDirectory, tempDirectory, photonDirectory, new File(photonDirectory, "elasticsearch/plugins")}) {
			if(!directory.exists())
				directory.mkdirs();
		}

		return mainDirectory;
	}

	public void recreateIndex() {
		deleteIndex();

		final Client client = this.getClient();
		final InputStream mappings = Thread.currentThread().getContextClassLoader().getResourceAsStream("mappings.json");
		final InputStream index_settings = Thread.currentThread().getContextClassLoader().getResourceAsStream("index_settings.json");

		try {
			client.admin().indices().prepareCreate("photon").setSettings(IOUtils.toString(index_settings)).get();
			client.admin().indices().preparePutMapping("photon").setType("place").setSource(IOUtils.toString(mappings)).get();
		} catch(IOException e) {
			log.error("cannot setup index, elastic search config files not readable", e);
		}
	}

	public DeleteIndexResponse deleteIndex() {
		try {
			return this.getClient().admin().indices().prepareDelete("photon").get();
		} catch(IndexMissingException e) {
			// index did not exist
			return null;
		}
	}
        
        public CloseIndexResponse closeIndex() {
		try {
			return this.getClient().admin().indices().prepareClose("photon").get();
		} catch(IndexMissingException e) {
			// index did not exist
			return null;
		}
	}
        
        public void waitForYellow() {
		this.getClient().admin().cluster().
                        health(new ClusterHealthRequest("photon").waitForYellowStatus()).actionGet();
	}
}

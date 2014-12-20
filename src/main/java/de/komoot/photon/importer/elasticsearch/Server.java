package de.komoot.photon.importer.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.plugins.PluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Helper class to start/stop elasticserach node and get elasticsearch clients
 *
 * @author felix
 */
@Slf4j
public class Server {

	private Node esNode;
	private String clusterName = "photon_v0.2";
	private File esDirectory;
	private File dumpDirectory;
	private File updateDirectory;
	private File tempDirectory;
	private File importDirectory;

	public Server(String clusterName, String mainDirectory) {
		try {
			if(SystemUtils.IS_OS_WINDOWS) {
				setupDirectories(new URL("file:///" + mainDirectory));
			} else {
				setupDirectories(new URL("file://" + mainDirectory));
			}
		} catch(Exception e) {
			log.error("Can't create directories: ", e);
		}
		this.clusterName = clusterName;
	}

	public Server start() {
		return start(false);
	}

	public Server start(boolean test) {
		ImmutableSettings.Builder sBuilder = ImmutableSettings.settingsBuilder();
		sBuilder.put("path.home", this.esDirectory.toString());
		sBuilder.put("network.host", "127.0.0.1"); // http://stackoverflow.com/a/15509589/1245622

		// default is 'local', 'none' means no data after node restart!
		if(test)
			sBuilder.put("gateway.type", "none");

		Settings settings = sBuilder.build();

		final String pluginPath = this.getClass().getResource("/elasticsearch-wordending-tokenfilter-0.0.1.zip").toExternalForm();
		PluginManager pluginManager = new PluginManager(new Environment(settings), pluginPath, PluginManager.OutputMode.VERBOSE, new TimeValue(60000));
		try {
			pluginManager.downloadAndExtract("ybon/elasticsearch-wordending-tokenfilter/0.0.1");
		} catch(IOException e) {
			log.debug("could not install ybon/elasticsearch-wordending-tokenfilter/0.0.1", e);
		}

		if(!test && false) {
			pluginManager = new PluginManager(new Environment(settings), null, PluginManager.OutputMode.VERBOSE, new TimeValue(30000));
			for(String pluginName : new String[]{"mobz/elasticsearch-head", "polyfractal/elasticsearch-inquisitor"}) {
				try {
					pluginManager.downloadAndExtract(pluginName);
				} catch(IOException e) {
				}
			}
		}

		NodeBuilder nBuilder = nodeBuilder().clusterName(clusterName).loadConfigSettings(true).
				settings(settings);

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

	/**
	 * returns an elasticsearch client
	 */
	public Client getClient() {
		return this.esNode.client();
	}

	private File setupDirectories(URL directoryName) throws IOException, URISyntaxException {
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
		final File scriptsDirectory = new File(esDirectory, "config/scripts");

		for(File directory : new File[]{
				esDirectory, dumpDirectory, updateDirectory, importDirectory, tempDirectory,
				photonDirectory, new File(photonDirectory, "elasticsearch/plugins"), scriptsDirectory
		}) {
			if(!directory.exists())
				directory.mkdirs();
		}

		// copy script directory to elastic search directory
		final ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Files.copy(loader.getResourceAsStream("scripts/general-score.mvel"), new File(scriptsDirectory, "general-score.mvel").toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(loader.getResourceAsStream("scripts/location-biased-score.mvel"), new File(scriptsDirectory, "location-biased-score.mvel").toPath(), StandardCopyOption.REPLACE_EXISTING);

		return mainDirectory;
	}

	public void recreateIndex() {
		deleteIndex();

		final Client client = this.getClient();
		final InputStream mappings = Thread.currentThread().getContextClassLoader().getResourceAsStream("mappings.json");
		final InputStream index_settings = Thread.currentThread().getContextClassLoader().getResourceAsStream("index_settings.json");

		try {
			client.admin().indices().prepareCreate("photon").setSettings(IOUtils.toString(index_settings)).execute().actionGet();
			client.admin().indices().preparePutMapping("photon").setType("place").setSource(IOUtils.toString(mappings)).execute().actionGet();
		} catch(IOException e) {
			log.error("cannot setup index, elastic search config files not readable", e);
		}
	}

	public DeleteIndexResponse deleteIndex() {
		try {
			return this.getClient().admin().indices().prepareDelete("photon").execute().actionGet();
		} catch(IndexMissingException e) {
			// index did not exist
			return null;
		}
	}
}

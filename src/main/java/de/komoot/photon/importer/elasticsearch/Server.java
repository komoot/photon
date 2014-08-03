package de.komoot.photon.importer.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Helper class to start/stop elasticserach node and get elasticsearch clients
 *
 * @author felix
 */
@Slf4j
public class Server {

	private Node esNode;
	private static final String clusterName = "photon_v0.1";
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
		ImmutableSettings.Builder sBuilder = ImmutableSettings.settingsBuilder();
		sBuilder.put("path.home", this.esDirectory.toString());
		sBuilder.put("network.host", "127.0.0.1"); // http://stackoverflow.com/a/15509589/1245622

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

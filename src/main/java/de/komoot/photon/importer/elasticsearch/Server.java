package de.komoot.photon.importer.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.PluginManager;

import java.io.File;
import java.io.IOException;
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
	private String clusterName;
	private File mainDirectory;
	private File esDirectory;
	private File dumpDirectory;
	private File updateDirectory;
	private File tempDirectory;
	private File importDirectory;

	public void start() {
		Settings settings = ImmutableSettings.settingsBuilder()
				.put("path.home", this.esDirectory.toString())
				.build();

		final String pluginPath = this.getClass().getResource("/elasticsearch-wordending-tokenfilter-0.0.1.zip").toExternalForm();
		PluginManager pluginManager = new PluginManager(new Environment(settings), pluginPath, PluginManager.OutputMode.VERBOSE, new TimeValue(30000));
		try {
			pluginManager.downloadAndExtract("ybon/elasticsearch-wordending-tokenfilter/0.0.1");
		} catch(IOException e) {
			log.debug("could not install ybon/elasticsearch-wordending-tokenfilter/0.0.1", e);
		}

		pluginManager = new PluginManager(new Environment(settings), null, PluginManager.OutputMode.VERBOSE, new TimeValue(30000));
		for(String pluginName : new String[]{"mobz/elasticsearch-head", "polyfractal/elasticsearch-inquisitor", "elasticsearch/marvel/latest"}) {
			try {
				pluginManager.downloadAndExtract(pluginName);
			} catch(IOException e) {
			}
		}

		this.esNode = nodeBuilder().clusterName(this.clusterName).loadConfigSettings(true).settings(settings).node();
		log.info("started elastic search node");
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

					log.info(String.format("Created snapshot: %s.zip", dumpLocation));
				} catch(ZipException e) {
					log.error("error creating zip file of snapshot", e);
				}
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("error creating snapshot", e);
			}
		});
	}

	public void importSnapshot(String dumpUrl, String dumpName) {
		File dump = new File(this.tempDirectory, dumpName + ".zip");
		String dumpLocation = "";
		try {
			FileUtils.copyFile(new File(new URL(dumpUrl).toURI()), dump);
		} catch(Exception e) {
			log.error("Error while loading dump (Is this the correct dump location?)", e);
		}

		try {
			ZipFile dumpZip = new ZipFile(dump);
			dumpLocation = this.importDirectory.getAbsolutePath() + dumpName;
			dumpZip.extractAll(dumpLocation);
		} catch(ZipException e) {

			log.error("Can´t unzip dump", e);
		}
		this.getClient().admin().cluster().getSnapshots(this.getClient().admin().cluster().prepareGetSnapshots(dumpLocation).setSnapshots(dumpName).request(), new ActionListener<GetSnapshotsResponse>() {
			@Override
			public void onResponse(GetSnapshotsResponse getSnapshotsResponse) {
				log.info("Import done!");
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("error creating snapshot", e);
			}
		});
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

	/**
	 * @param clusterName name of the elasticsearch cluster
	 */
	public Server(String clusterName, String mainDirectory) {

		try {
			this.mainDirectory = setupDirectories(new URL("file://" + mainDirectory));
		} catch(MalformedURLException e) {
			log.error("Can´t create directories");
		}
		this.clusterName = "defdeweef";
	}
}

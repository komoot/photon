package de.komoot.photon.importer.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
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
import java.io.FileOutputStream;
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

	public void importSnapshot(String urlString) {
		URL url = null;
		try {
			url = new URL(urlString);
		} catch(MalformedURLException e) {
			throw new RuntimeException("invalid snapshot url", e);
		}
		File file;
		try {
			file = new File(url.toURI());
		} catch(Exception e) {
			// url did not refer to a local file, download it first to unzip
			try {
				file = File.createTempFile("temp-file-name", ".tmp");
				IOUtils.copyLarge(url.openStream(), new FileOutputStream(file));
			} catch(IOException ioe) {
				throw new RuntimeException("cannot create temp file for snapshot", ioe);
			}
		}

		String dumpLocation;
		String dumpName = "photon_snapshot_2014_05";
		try {
			ZipFile dumpZip = new ZipFile(file);
			dumpLocation = this.importDirectory.getAbsolutePath();
			dumpZip.extractAll(dumpLocation);
		} catch(ZipException e) {
			throw new RuntimeException("error unzipping snapshot", e);
		}

		String repository = dumpLocation + "/" + dumpName;
		String snapshot = dumpName;

		this.getClient().admin().cluster().getSnapshots(this.getClient().admin().cluster().prepareGetSnapshots(repository).setSnapshots(snapshot).request(), new ActionListener<GetSnapshotsResponse>() {
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

package de.komoot.photon.elasticsearch;

import de.komoot.photon.CommandLineArgs;
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
import org.elasticsearch.plugins.PluginManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Helper class to start/stop elasticserach node and get elasticsearch clients
 *
 * @author felix
 */
@Slf4j
public class Server {
        private Node esNode;
	private Client esClient;
	private String clusterName;
	private File esDirectory;
	private final boolean isTest;
	private final String[] languages;
        private String transportAddresses;

	public Server(CommandLineArgs args) {            
		this(args.getCluster(), args.getDataDirectory(), args.getLanguages(), args.getTransportAddresses(), false);
	}

	public Server(String clusterName, String mainDirectory, String languages, String transportAddresses, boolean isTest) {
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
		this.languages = languages.split(",");
                this.transportAddresses = transportAddresses;
		this.isTest = isTest;
	}

	public Server start() {
		ImmutableSettings.Builder sBuilder = ImmutableSettings.settingsBuilder();
		sBuilder.put("path.home", this.esDirectory.toString());
		sBuilder.put("network.host", "127.0.0.1"); // http://stackoverflow.com/a/15509589/1245622
                sBuilder.put("script.disable_dynamic", "true");
                sBuilder.put("script.groovy.sandbox.enabled", "false");

		// default is 'local', 'none' means no data after node restart!
		if(isTest)
                    sBuilder.put("gateway.type", "none");		

                if(transportAddresses != null && !transportAddresses.isEmpty()) {                    
                    sBuilder.put("cluster.name", clusterName);
                    TransportClient trClient = new TransportClient(sBuilder.build(), true);
                    List<String> addresses = Arrays.asList(transportAddresses.split(","));
                    for(String tAddr : addresses) {
                        int index = tAddr.indexOf(":");
                        if(index >= 0) {                            
                            int port = Integer.parseInt(tAddr.substring(index + 1));
                            String addrStr = tAddr.substring(0, index);
                            trClient.addTransportAddress(new InetSocketTransportAddress(addrStr, port));
                        } else {
                            trClient.addTransportAddress(new InetSocketTransportAddress(tAddr, 9300));
                        }                        
                    }
                                        
                    esClient = trClient;
                    
                    log.info("started elastic search client connected to " + addresses);                    
                } else {
                    Settings settings = sBuilder.build();

                    final String pluginPath = this.getClass().getResource("/elasticsearch-wordending-tokenfilter-0.0.1.zip").toExternalForm();
                    PluginManager pluginManager = new PluginManager(new Environment(settings), pluginPath, PluginManager.OutputMode.VERBOSE, new TimeValue(30000));
                    try {
                        pluginManager.downloadAndExtract("ybon/elasticsearch-wordending-tokenfilter/0.0.1");
                    } catch(IOException e) {
                        log.debug("could not install ybon/elasticsearch-wordending-tokenfilter/0.0.1", e);
                    }

                    if(!isTest) {
                        pluginManager = new PluginManager(new Environment(settings), null, PluginManager.OutputMode.VERBOSE, new TimeValue(30000));
                        for(String pluginName : new String[]{"mobz/elasticsearch-head", "polyfractal/elasticsearch-inquisitor"}) {
                            try {
                                    pluginManager.downloadAndExtract(pluginName);
                            } catch(IOException e) {
                                    log.error(String.format("cannot install plugin: %s: %s", pluginName, e));
                            }
                        }
                    }
                    esNode = nodeBuilder().clusterName(clusterName).loadConfigSettings(true).settings(settings).node();
                    log.info("started elastic search node");
                    esClient = esNode.client();
                }		
		return this;
	}

	/**
	 * stops the elasticsearch node
	 */
	public void shutdown() {
                if(esNode != null)
                    esNode.close();
                
		esClient.close();
	}

	/**
	 * returns an elasticsearch client
	 */
	public Client getClient() {
		return esClient;
	}

	private void setupDirectories(URL directoryName) throws IOException, URISyntaxException {
		final File mainDirectory = new File(directoryName.toURI());
		final File photonDirectory = new File(mainDirectory, "photon_data");
		this.esDirectory = new File(photonDirectory, "elasticsearch");
		final File pluginDirectory = new File(esDirectory, "plugins");
		final File scriptsDirectory = new File(esDirectory, "config/scripts");

		for(File directory : new File[]{esDirectory, photonDirectory, pluginDirectory, scriptsDirectory}) {
			directory.mkdirs();
		}

		// copy script directory to elastic search directory
		final ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Files.copy(loader.getResourceAsStream("scripts/general-score.groovy"), new File(scriptsDirectory, "general-score.groovy").toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(loader.getResourceAsStream("scripts/location-biased-score.groovy"), new File(scriptsDirectory, "location-biased-score.groovy").toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	public void recreateIndex() throws IOException {
		deleteIndex();

		final Client client = this.getClient();
		final InputStream mappings = Thread.currentThread().getContextClassLoader().getResourceAsStream("mappings.json");
		final InputStream index_settings = Thread.currentThread().getContextClassLoader().getResourceAsStream("index_settings.json");

		String mappingsString = IOUtils.toString(mappings);
		JSONObject mappingsJSON = new JSONObject(mappingsString);

		// add all langs to the mapping
		mappingsJSON = addLangsToMapping(mappingsJSON);
		client.admin().indices().prepareCreate("photon").setSettings(IOUtils.toString(index_settings)).execute().actionGet();
		client.admin().indices().preparePutMapping("photon").setType("place").setSource(mappingsJSON.toString()).execute().actionGet();
		log.info("mapping created: " + mappingsJSON.toString());
	}

	public DeleteIndexResponse deleteIndex() {
		try {
			return this.getClient().admin().indices().prepareDelete("photon").execute().actionGet();
		} catch(IndexMissingException e) {
			// index did not exist
			return null;
		}
	}

	private JSONObject addLangsToMapping(JSONObject mappingsObject) {
		// define collector json strings
		String copyToCollectorString = "{\"type\":\"string\",\"index\":\"no\",\"copy_to\":[\"collector.{lang}\"]}";
		String nameToCollectorString = "{\"type\":\"string\",\"index\":\"no\",\"fields\":{\"ngrams\":{\"type\":\"string\",\"index_analyzer\":\"index_ngram\"},\"raw\":{\"type\":\"string\",\"index_analyzer\":\"index_raw\"}},\"copy_to\":[\"collector.{lang}\"]}";
		String collectorString = "{\"type\":\"string\",\"index\":\"no\",\"fields\":{\"ngrams\":{\"type\":\"string\",\"index_analyzer\":\"index_ngram\"},\"raw\":{\"type\":\"string\",\"index_analyzer\":\"index_raw\"}},\"copy_to\":[\"collector.{lang}\"]}}},\"street\":{\"type\":\"object\",\"properties\":{\"default\":{\"index\":\"no\",\"type\":\"string\",\"copy_to\":[\"collector.default\"]}";

		JSONObject placeObject = mappingsObject.optJSONObject("place");
		JSONObject propertiesObject = placeObject == null ? null : placeObject.optJSONObject("properties");

		if(propertiesObject != null) {
			for(String lang : languages) {
				// create lang-specific json objects
				JSONObject copyToCollectorObject = new JSONObject(copyToCollectorString.replace("{lang}", lang));
				JSONObject nameToCollectorObject = new JSONObject(nameToCollectorString.replace("{lang}", lang));
				JSONObject collectorObject = new JSONObject(collectorString.replace("{lang}", lang));

				// add language specific tags to the collector
				propertiesObject = addToCollector("city", propertiesObject, copyToCollectorObject, lang);
				propertiesObject = addToCollector("context", propertiesObject, copyToCollectorObject, lang);
				propertiesObject = addToCollector("country", propertiesObject, copyToCollectorObject, lang);
				propertiesObject = addToCollector("state", propertiesObject, copyToCollectorObject, lang);
				propertiesObject = addToCollector("street", propertiesObject, copyToCollectorObject, lang);
				propertiesObject = addToCollector("name", propertiesObject, nameToCollectorObject, lang);

				// add language specific collector to default for name
				JSONObject name = propertiesObject.optJSONObject("name");
				JSONObject nameProperties = name == null ? null : name.optJSONObject("properties");
				if(nameProperties != null) {
					JSONObject defaultObject = nameProperties.optJSONObject("default");
					JSONArray copyToArray = defaultObject.optJSONArray("copy_to");
					copyToArray.put("name." + lang);

					defaultObject.put("copy_to", copyToArray);
					nameProperties.put("default", defaultObject);
					name.put("properties", nameProperties);
					propertiesObject.put("name", name);
				}

				// add language specific collector
				propertiesObject = addToCollector("collector", propertiesObject, collectorObject, lang);
			}
			placeObject.put("properties", propertiesObject);
			return mappingsObject.put("place", placeObject);
		}

		log.error("cannot add languages to mapping.json, please double-check the mappings.json or the language values supplied");
		return null;
	}

	private JSONObject addToCollector(String key, JSONObject properties, JSONObject collectorObject, String lang) {
		JSONObject keyObject = properties.optJSONObject(key);
		JSONObject keyProperties = keyObject == null ? null : keyObject.optJSONObject("properties");
		if(keyProperties != null) {
			keyProperties.put(lang, collectorObject);
			keyObject.put("properties", keyProperties);
			return properties.put(key, keyObject);
		}
		return properties;
	}
}

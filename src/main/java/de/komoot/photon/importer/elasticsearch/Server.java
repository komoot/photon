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
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Helper class to start/stop elasticserach node and get elasticsearch clients
 *
 * @author felix
 */
@Slf4j
public class Server {

	private Node esNode;
	private String clusterName = "photon_v0.1";
	private File esDirectory;
	private File dumpDirectory;
	private File updateDirectory;
	private File tempDirectory;
	private File importDirectory;
        private String[] langs;

	public Server(String clusterName, String mainDirectory, String langs) {
		try {
			setupDirectories(new URL("file://" + mainDirectory));
		} catch(MalformedURLException e) {
			log.error("Can´t create directories");
		} catch(Exception ex)
		{
			try {
				setupDirectories(new URL("file:///" + mainDirectory)); //Enable running on windows.
			} catch(MalformedURLException e) {
				log.error("Can´t create directories");
			}
		}
        this.clusterName = clusterName;
        this.langs = langs.split(",");
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
                        for(String lang : langs) {
                                // create lang-specific json objects
                                JSONObject copyToCollectorObject = new JSONObject(copyToCollectorString.replace("{lang}", lang));
                                JSONObject nameToCollectorObject = new JSONObject(nameToCollectorString.replace("{lang}", lang));
                                JSONObject collectorObject = new JSONObject(collectorString.replace("{lang}", lang));

                                // add language specific tags to the collector
                                propertiesObject = addToCollector("city", propertiesObject, copyToCollectorObject, lang);
                                propertiesObject = addToCollector("context", propertiesObject, copyToCollectorObject, lang);
                                propertiesObject = addToCollector("country", propertiesObject, copyToCollectorObject, lang);
                                propertiesObject = addToCollector("street", propertiesObject, copyToCollectorObject, lang);
                                propertiesObject = addToCollector("name", propertiesObject, nameToCollectorObject, lang);

                                // add language specific collector to default for name
                                JSONObject name = propertiesObject.optJSONObject("name");
                                JSONObject nameProperties = name == null ? null : name.optJSONObject("properties");
                                if(nameProperties != null) {
                                        JSONObject defaultObject = nameProperties.optJSONObject("default");
                                        JSONArray copyToArray = defaultObject == null ? null : defaultObject.optJSONArray("copy_to");
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
                
                log.error("cannot add langs to mapping.json, please double-check the mappings.json or the language values supplied");
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

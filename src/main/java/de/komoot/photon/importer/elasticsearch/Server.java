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

	public void recreateIndex() {
		deleteIndex();

		final Client client = this.getClient();
		final InputStream mappings = Thread.currentThread().getContextClassLoader().getResourceAsStream("mappings.json");
		final InputStream index_settings = Thread.currentThread().getContextClassLoader().getResourceAsStream("index_settings.json");

		try {
                        // get mappings as JSONObject
                        String mappingsString = IOUtils.toString(mappings);
                        JSONObject mappingsJSON = new JSONObject(mappingsString);
                        
                        // add all langs to the mapping
                        mappingsJSON = addLangsToMapping(mappingsJSON);
                        
			client.admin().indices().prepareCreate("photon").setSettings(IOUtils.toString(index_settings)).execute().actionGet();
			client.admin().indices().preparePutMapping("photon").setType("place").setSource(mappingsJSON.toString()).execute().actionGet();
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
        
        private JSONObject addLangsToMapping(JSONObject mappingsObject) {
                // define collector json strings
                String copyToCollectorString = "{\"type\":\"string\",\"index\":\"no\",\"copy_to\":[\"collector.{lang}\"]}";
                String nameToCollectorString = "{\"type\":\"string\",\"index\":\"no\",\"fields\":{\"ngrams\":{\"type\":\"string\",\"index_analyzer\":\"index_ngram\"},\"raw\":{\"type\":\"string\",\"index_analyzer\":\"index_raw\"}},\"copy_to\":[\"collector.{lang}\"]}";
                String collectorString = "{\"type\":\"string\",\"index\":\"no\",\"fields\":{\"ngrams\":{\"type\":\"string\",\"index_analyzer\":\"index_ngram\"},\"raw\":{\"type\":\"string\",\"index_analyzer\":\"index_raw\"}},\"copy_to\":[\"collector.{lang}\"]}}},\"street\":{\"type\":\"object\",\"properties\":{\"default\":{\"index\":\"no\",\"type\":\"string\",\"copy_to\":[\"collector.default\"]}";
                
                if(mappingsObject != null) {
                        JSONObject placeObject = mappingsObject.optJSONObject("place");
                        JSONObject propertiesObject = placeObject == null ? null : placeObject.optJSONObject("properties");
                        
                        if(propertiesObject != null) {
                                for(String lang : langs) {
                                        // create lang-specific json objects
                                        JSONObject copyToCollectorObject = new JSONObject(copyToCollectorString.replace("{lang}", lang));
                                        JSONObject nameToCollectorObject = new JSONObject(nameToCollectorString.replace("{lang}", lang));
                                        JSONObject collectorObject = new JSONObject(collectorString.replace("{lang}", lang));
                                    
                                        // add language specific city to the collector
                                        JSONObject city = propertiesObject.optJSONObject("city");
                                        JSONObject cityProperties = city == null ? null : city.optJSONObject("properties");
                                        if(cityProperties != null) {
                                                cityProperties.put(lang, copyToCollectorObject);
                                                city.put("properties", cityProperties);
                                                propertiesObject.put("city", city);
                                        }
                                        
                                        // add language specific context to the collector
                                        JSONObject context = propertiesObject.optJSONObject("context");
                                        JSONObject contextProperties = context == null ? null : context.optJSONObject("properties");
                                        if(contextProperties != null) {
                                                contextProperties.put(lang, copyToCollectorObject);
                                                context.put("properties", contextProperties);
                                                propertiesObject.put("context", context);
                                        }
                                        
                                        // add language specific country to the collector
                                        JSONObject country = propertiesObject.optJSONObject("country");
                                        JSONObject countryProperties = country == null ? null : country.optJSONObject("properties");
                                        if(countryProperties != null) {
                                                countryProperties.put(lang, copyToCollectorObject);
                                                country.put("properties", countryProperties);
                                                propertiesObject.put("country", country);
                                        }
                                        
                                        // add language specific street to the collector
                                        JSONObject street = propertiesObject.optJSONObject("street");
                                        JSONObject streetProperties = street == null ? null : street.optJSONObject("properties");
                                        if(streetProperties != null) {
                                                streetProperties.put(lang, copyToCollectorObject);
                                                street.put("properties", streetProperties);
                                                propertiesObject.put("street", street);
                                        }
                                        
                                        // add language specific name to the collector
                                        JSONObject name = propertiesObject.optJSONObject("name");
                                        JSONObject nameProperties = name == null ? null : name.optJSONObject("properties");
                                        if(nameProperties != null) {
                                                nameProperties.put(lang, nameToCollectorObject);
                                                JSONObject defaultObject = nameProperties.optJSONObject("default");
                                                
                                                // add language specific collector to default
                                                JSONArray copyToArray = defaultObject == null ? null : defaultObject.optJSONArray("copy_to");
                                                copyToArray.put("name." + lang);
                                                
                                                defaultObject.put("copy_to", copyToArray);
                                                nameProperties.put("default", defaultObject);
                                                name.put("properties", nameProperties);
                                                propertiesObject.put("name", name);
                                        }
                                        
                                        // add language specific collector
                                        JSONObject collector = propertiesObject.optJSONObject("collector");
                                        JSONObject collectorProperties = collector == null ? null : collector.optJSONObject("properties");
                                        if(collectorProperties != null) {
                                                collectorProperties.put(lang, collectorObject);
                                                collector.put("properties", collectorProperties);
                                                propertiesObject.put("collector", collector);
                                        }
                                }
                                placeObject.put("properties", propertiesObject);
                                return mappingsObject.put("place", placeObject);
                        }
                }
                log.error("cannot add langs to mapping.json, please double-check the mappings.json or the language values supplied");
                return null;
        }
}

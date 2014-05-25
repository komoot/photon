package de.komoot.photon.importer;

import com.beust.jcommander.JCommander;
import com.google.common.base.Joiner;
import de.komoot.photon.importer.elasticsearch.Importer;
import de.komoot.photon.importer.elasticsearch.Searcher;
import de.komoot.photon.importer.elasticsearch.Server;
import de.komoot.photon.importer.json.JsonDumper;
import de.komoot.photon.importer.nominatim.NominatimConnector;
import de.komoot.photon.importer.nominatim.NominatimUpdater;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.ImmutableSet;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

import static spark.Spark.get;
import static spark.Spark.setPort;

@Slf4j
public class App {
	private static final Set<String> supportedLanguages = ImmutableSet.of("de", "en", "fr", "it");
	private static final ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] rawArgs) {
		CommandLineArgs args = new CommandLineArgs();
		new JCommander(args, rawArgs);

		final Server esServer = new Server(args.getDataDirectory());
		esServer.start();

		Client esNodeClient = esServer.getClient();

		if(args.isNominatimImport()) {
			esServer.recreateIndex(); // dump previous data
			Importer importer = new Importer(esNodeClient);
			NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
			nominatimConnector.setImporter(importer);
			nominatimConnector.readEntireDatabase();
		}

		if(args.isJsonDump()) {
			try {
				final JsonDumper jsonDumper = new JsonDumper("/tmp/photon_dump", args.getJsonLines());
				NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
				nominatimConnector.setImporter(jsonDumper);
				nominatimConnector.readEntireDatabase();
			} catch(FileNotFoundException e) {
				log.error("cannot create dump", e);
			}
		}

		if(args.getCreateSnapshot() != null) {
			esServer.createSnapshot(args.getCreateSnapshot());
		}

		if(args.isDeleteIndex()) {
			esServer.recreateIndex();
		}

		if(args.getImportSnapshot() != null) {
			esServer.deleteIndex();
			esServer.importSnapshot(args.getImportSnapshot(), "photon_snapshot_2014_05");
			//esServer.importSnapshot(args.getImportSnapshot());
		}

		final NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
		de.komoot.photon.importer.Updater updater = new de.komoot.photon.importer.elasticsearch.Updater(esNodeClient);
		nominatimUpdater.setUpdater(updater);

		setPort(2322);
		get(new Route("/nominatim-update") {
			@Override
			public Object handle(Request request, Response response) {
				Thread nominatimUpdaterThread = new Thread() {
					@Override
					public void run() {
						nominatimUpdater.update();
					}
				};
				nominatimUpdaterThread.start();
				return "nominatim update started (more information in console output) ...";
			}
		});

		final Searcher searcher = new Searcher(esNodeClient);
		get(new Route("api") {
			@Override
			public String handle(Request request, Response response) {
				// parse query term
				String query = request.queryParams("q");
				if(query == null) {
					halt(400, "missing search term 'q': /?q=berlin");
				}

				// parse preferred language
				String lang = request.queryParams("lang");
				if(lang == null) lang = "en";
				if(!supportedLanguages.contains(lang)) {
					halt(400, "language " + lang + " is not supported, supported languages are: " + Joiner.on(", ").join(supportedLanguages));
				}

				// parse location bias
				Double lon = null, lat = null;
				try {
					lon = Double.valueOf(request.queryParams("lon"));
					lat = Double.valueOf(request.queryParams("lat"));
				} catch(Exception nfe) {
				}

				// parse limit for search results
				int limit = 15;
				try {
					limit = Math.min(50, Integer.parseInt(request.queryParams("limit")));
				} catch(Exception e) {
				}

				List<JSONObject> results = searcher.search(query, lang, lon, lat, limit, true);
				if(results.isEmpty()) {
					// try again, but less restrictive
					results = searcher.search(query, lang, lon, lat, limit, false);
				}

				// build geojson
				final JSONObject collection = new JSONObject();
				collection.put("type", "FeatureCollection");
				collection.put("features", new JSONArray(results));

				response.type("application/json; charset=utf-8");

				if(request.queryParams("debug") != null)
					return collection.toString(4);

				return collection.toString();
			}
		});
	}
}

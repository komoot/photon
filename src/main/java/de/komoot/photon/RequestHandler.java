package de.komoot.photon;

import com.google.common.base.Joiner;
import de.komoot.photon.elasticsearch.Searcher;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * date: 31.10.14
 *
 * @author christoph
 */
public class RequestHandler extends Route {
	private final Searcher searcher;
	private final Set<String> supportedLanguages;

	protected RequestHandler(String path, Searcher searcher, String languages) {
		super(path);
		this.searcher = searcher;
		this.supportedLanguages = new HashSet<String>(Arrays.asList(languages.split(",")));
	}

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
		int limit;
		try {
			limit = Math.min(50, Integer.parseInt(request.queryParams("limit")));
		} catch(Exception e) {
			limit = 15;
		}

        String osmKey = request.queryParams("osm_key");
        String osmValue = request.queryParams("osm_value");

        List<JSONObject> results = searcher.search(query, lang, lon, lat, osmKey,osmValue,limit, true);
		if(results.isEmpty()) {
			// try again, but less restrictive
			results = searcher.search(query, lang, lon, lat, osmKey,osmValue,limit, false);
		}

		// build geojson
		final JSONObject collection = new JSONObject();
		collection.put("type", "FeatureCollection");
		collection.put("features", new JSONArray(results));

		response.type("application/json; charset=utf-8");
		response.header("Access-Control-Allow-Origin", "*");

		if(request.queryParams("debug") != null)
			return collection.toString(4);

		return collection.toString();
	}
}

package de.komoot.photon;

import spark.Request;
import spark.Response;
import spark.Route;

/**
 * date: 02.02.16
 *
 * @author christoph
 */
public class OptionsRequestHandler extends Route {
	protected OptionsRequestHandler(String path) {
		super(path);
	}

	@Override
	public Object handle(Request request, Response response) {
		response.header("Access-Control-Allow-Origin", "*");
		response.header("Access-Control-Allow-Methods", "GET, OPTIONS");
		response.header("Access-Control-Max-Age", "2592000"); // 30 days
		return "";
	}
}

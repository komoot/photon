package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import spark.Request;

/**
 *
 * @author svantulden
 */
public class ReverseRequestFactory {
	private final LanguageChecker languageChecker;
	private final static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
	
	protected static HashSet<String> m_hsRequestQueryParams = new HashSet<>(Arrays.asList("lang", "lon", "lat", "radius", "query_string_filter", "distance_sort", "limit"));

	public ReverseRequestFactory(Set<String> supportedLanguages) {
		this.languageChecker = new LanguageChecker(supportedLanguages);
	}
	   
	public <R extends ReverseRequest> R create(Request webRequest) throws BadRequestException {
		
		
		for(String queryParam: webRequest.queryParams())
			if(!m_hsRequestQueryParams.contains(queryParam))
				throw new BadRequestException(400, "unknown query parameter '" + queryParam + "'.  Allowed parameters are: " + m_hsRequestQueryParams);
		
		
		String language = webRequest.queryParams("lang");
		language = language == null ? "en" : language;
		languageChecker.apply(language);
		
		Point location;
		try {
			Double lon = Double.valueOf(webRequest.queryParams("lon"));
			if(lon > 180.0 || lon < -180.00){
				throw new BadRequestException(400, "invalid search term 'lon', expected number >= -180.0 and <= 180.0");
			}
			Double lat = Double.valueOf(webRequest.queryParams("lat"));
			if(lat > 90.0 || lat < -90.00){
				throw new BadRequestException(400, "invalid search term 'lat', expected number >= -90.0 and <= 90.0");
			}
			location = geometryFactory.createPoint(new Coordinate(lon, lat));
		} catch (NumberFormatException nfe) {
			throw new BadRequestException(400, "invalid search term 'lat' and/or 'lon': /?lat=51.5&lon=8.0");
		} catch (NullPointerException nfe) {
			throw new BadRequestException(400, "missing search term 'lat' and/or 'lon': /?lat=51.5&lon=8.0");
		}

		Double radius = 1d;
		String radiusParam = webRequest.queryParams("radius");
		if(radiusParam != null){
			try {
				radius = Double.valueOf(radiusParam);
			} catch (Exception nfe) {
				throw new BadRequestException(400, "invalid search term 'radius', expected a number.");
			}
			if(radius <= 0){
				throw new BadRequestException(400, "invalid search term 'radius', expected a strictly positive number.");
			}else{
				// limit search radius to 5000km
				radius = Math.min(radius, 5000d);
			}
		}
		
		String queryStringFilter = webRequest.queryParams("query_string_filter");
		
		Boolean locationDistanceSort = true;
		try {
			if(webRequest.queryParams("distance_sort") == null)
				locationDistanceSort = true;
			else
				locationDistanceSort = Boolean.valueOf(webRequest.queryParams("distance_sort"));
			
		} catch (Exception nfe) {
			throw new BadRequestException(400, "invalid search term 'query_string_filter', can only be true|false");
		}

		Integer limit = 1;
		String limitParam = webRequest.queryParams("limit");
		if(limitParam != null){
			try {
				limit = Integer.valueOf(limitParam);
			} catch (Exception nfe) {
				throw new BadRequestException(400, "invalid search term 'limit', expected an integer.");
			}
			if(limit <= 0){
				throw new BadRequestException(400, "invalid search term 'limit', expected a strictly positive integer.");
			}else{
				// limit number of results to 50
				limit = Math.min(limit, 50);
			}
		}
		
		
		
		ReverseRequest reverseRequest = new ReverseRequest(location, language, radius, queryStringFilter, limit, locationDistanceSort);
		
		return (R) reverseRequest;
	}
}

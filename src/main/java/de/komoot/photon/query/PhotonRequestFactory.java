package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import spark.QueryParamsMap;
import spark.Request;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonRequestFactory {
    public PhotonRequest create(Request webRequest) throws BadRequestException {
        String query = webRequest.queryParams("q");
        if (query == null) throw new BadRequestException("missing search term 'q': /?q=berlin");
        Integer limit;
        try {
            limit = Integer.valueOf(webRequest.queryParams("limit"));
        } catch (NumberFormatException e) {
            limit = 15;
        }
        Double lon = null;
        Double lat = null;
        Point locationForBias = null;
        try {
            lon = Double.valueOf(webRequest.queryParams("lon"));
            lat = Double.valueOf(webRequest.queryParams("lat"));
            locationForBias = new GeometryFactory(new PrecisionModel(), 4326).createPoint(new Coordinate(lon, lat));
        } catch (Exception nfe) {
            //ignore
        }
        QueryParamsMap tagFiltersQueryMap = webRequest.queryMap("osm_tag");
        if (tagFiltersQueryMap == null) return new PhotonRequest(query, limit, locationForBias);
        FilteredPhotonRequest photonRequest = new FilteredPhotonRequest(query, limit, locationForBias);
        String[] tagFilters = tagFiltersQueryMap.values();
        setUpTagFilters(photonRequest,tagFilters);
        return photonRequest;
    }
    
    private static void setUpTagFilters(FilteredPhotonRequest request,String[] tagFilters){
        for (String tagFilter : tagFilters) {
            if (!tagFilter.contains(":")){
                //only tag
                if (tagFilter.startsWith("!")){
                    request.key(tagFilter.substring(1));
                }
            }
        }
    }

}

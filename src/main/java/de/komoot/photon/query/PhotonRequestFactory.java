package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import spark.QueryParamsMap;
import spark.Request;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonRequestFactory {

    private final LanguageChecker languageChecker;

    public PhotonRequestFactory(Set<String> supportedLanguages) {
        this.languageChecker = new LanguageChecker(supportedLanguages);
    }

    public <R extends PhotonRequest> R create(Request webRequest) throws BadRequestException {
        String language = webRequest.queryParams("lang");
        languageChecker.check(language);
        String query = webRequest.queryParams("q");
        if (query == null) throw new BadRequestException(400, "missing search term 'q': /?q=berlin");
        Integer limit;
        try {
            limit = Integer.valueOf(webRequest.queryParams("limit"));
        } catch (NumberFormatException e) {
            limit = 15;
        }
        Point locationForBias = null;
        try {
            Double lon = Double.valueOf(webRequest.queryParams("lon"));
            Double lat = Double.valueOf(webRequest.queryParams("lat"));
            locationForBias = new GeometryFactory(new PrecisionModel(), 4326).createPoint(new Coordinate(lon, lat));
        } catch (Exception nfe) {
            //ignore
        }
        QueryParamsMap tagFiltersQueryMap = webRequest.queryMap("osm_tag");
        if (!new CheckIfFilteredRequest().execute(tagFiltersQueryMap)) {
            return (R) new PhotonRequest(query, limit, locationForBias, language);
        }
        FilteredPhotonRequest photonRequest = new FilteredPhotonRequest(query, limit, locationForBias, language);
        String[] tagFilters = tagFiltersQueryMap.values();
        setUpTagFilters(photonRequest, tagFilters);
        return (R) photonRequest;
    }

    private void setUpTagFilters(FilteredPhotonRequest request, String[] tagFilters) {
        for (String tagFilter : tagFilters) {
            if (tagFilter.contains(":")) {
                //might be tag and value OR just value.
                if (tagFilter.startsWith("!")) {
                    //exclude
                    String keyValueCandidate = tagFilter.substring(1);
                    if (keyValueCandidate.startsWith(":")) {
                        //just value
                        request.notValues(keyValueCandidate.substring(1));
                    } else {
                        //key and value
                        String[] keyAndValue = keyValueCandidate.split(":");
                        Set<String> valuesToExclude = request.notTags().get(keyAndValue[0]);
                        if (valuesToExclude == null) valuesToExclude = new HashSet<String>();
                        valuesToExclude.add(keyAndValue[1]);
                        request.notTags(keyAndValue[0], valuesToExclude);
                    }
                } else {
                    //include
                    if (tagFilter.startsWith(":")) {
                        //just value
                        request.values(tagFilter.substring(1));
                    } else {
                        //key and value
                        String[] keyAndValue = tagFilter.split(":");
                        Set<String> valuesToInclude = request.tags().get(keyAndValue[0]);
                        if (valuesToInclude == null) valuesToInclude = new HashSet<String>();
                        valuesToInclude.add(keyAndValue[1]);
                        request.tags(keyAndValue[0], valuesToInclude);
                    }
                }
            } else {
                //only tag
                if (tagFilter.startsWith("!")) {
                    request.notKeys(tagFilter.substring(1));
                } else {
                    request.keys(tagFilter);
                }
            }
        }
    }

}

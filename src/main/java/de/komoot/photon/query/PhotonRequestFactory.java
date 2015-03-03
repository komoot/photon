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
 * A factory that creates a {@link PhotonRequest} from a {@link Request web request}
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonRequestFactory {

    private final LanguageChecker languageChecker;
    private final static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public PhotonRequestFactory(Set<String> supportedLanguages) {
        this.languageChecker = new LanguageChecker(supportedLanguages);
    }

    public <R extends PhotonRequest> R create(Request webRequest) throws BadRequestException {
        String language = webRequest.queryParams("lang");
        language = language == null ? "en" : language;
        languageChecker.apply(language);
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
            locationForBias = geometryFactory.createPoint(new Coordinate(lon, lat));
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
                        String excludeKey = keyAndValue[0];
                        String value = keyAndValue[1].startsWith("!") ? keyAndValue[1].substring(1) : keyAndValue[1];
                        Set<String> valuesToExclude = request.notTags().get(excludeKey);
                        if (valuesToExclude == null) valuesToExclude = new HashSet<String>();
                        valuesToExclude.add(value);
                        request.notTags(excludeKey, valuesToExclude);
                    }
                } else {
                    //include key, not sure about value
                    if (tagFilter.startsWith(":")) {
                        //just value

                        String valueCandidate = tagFilter.substring(1);
                        if (valueCandidate.startsWith("!")){
                            //exclude value
                            request.notValues(valueCandidate.substring(1));
                        }else {
                            //include value
                            request.values(valueCandidate);
                        }
                    } else {
                        //key and value
                        String[] keyAndValue = tagFilter.split(":");

                        String key = keyAndValue[0];
                        String value = keyAndValue[1];
                        if (value.startsWith("!")) {
                            //exclude value
                            Set<String> tagKeysValuesNotIncluded = request.tagNotValues().get(key);
                            if (tagKeysValuesNotIncluded == null) tagKeysValuesNotIncluded = new HashSet<String>();
                            tagKeysValuesNotIncluded.add(value.substring(1));
                            request.tagNotValues(key, tagKeysValuesNotIncluded);
                        } else {
                            //include value
                            Set<String> valuesToInclude = request.tags().get(key);
                            if (valuesToInclude == null) valuesToInclude = new HashSet<String>();
                            valuesToInclude.add(value);
                            request.tags(key, valuesToInclude);
                        }
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

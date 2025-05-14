package de.komoot.photon.query;

import spark.Request;

import java.util.List;
import java.util.Set;

public class SimpleSearchRequestFactory extends SearchRequestFactoryBase {
    private static final Set<String> REQUEST_QUERY_PARAMS = Set.of("lang", "q", "lon", "lat",
            "limit", "osm_tag", "location_bias_scale", "bbox", "debug", "zoom", "layer", "geometry");


    public SimpleSearchRequestFactory(List<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        super(supportedLanguages, defaultLanguage, maxResults, supportGeometries);
    }

    public SimpleSearchRequest create(Request webRequest) throws BadRequestException {
        for (String queryParam : webRequest.queryParams())
            if (!REQUEST_QUERY_PARAMS.contains(queryParam))
                throw new BadRequestException(400, "unknown query parameter '" + queryParam + "'.  Allowed parameters are: " + REQUEST_QUERY_PARAMS);

        String query = webRequest.queryParams("q");
        if (query == null) {
            throw new BadRequestException(400, "missing search term 'q': /?q=berlin");
        }

        SimpleSearchRequest request = new SimpleSearchRequest();
        request.setQuery(query);

        addCommonParameters(webRequest, request);

        return request;
    }
}

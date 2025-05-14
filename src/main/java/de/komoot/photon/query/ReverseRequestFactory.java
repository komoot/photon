package de.komoot.photon.query;

import de.komoot.photon.searcher.TagFilter;
import spark.QueryParamsMap;
import spark.Request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Factory that creates a {@link ReverseRequest} from a {@link Request web request}
 */
public class ReverseRequestFactory implements RequestFactory<ReverseRequest> {

    private static final HashSet<String> REQUEST_QUERY_PARAMS = new HashSet<>(Arrays.asList("lang", "lon", "lat", "radius",
            "query_string_filter", "distance_sort", "limit", "layer", "osm_tag", "debug", "geometry"));
    private static final LocationParamConverter mandatoryLocationParamConverter = new LocationParamConverter(true);

    private final RequestLanguageResolver languageResolver;
    private final LayerParamValidator layerParamValidator;
    private final int maxResults;

    public ReverseRequestFactory(List<String> supportedLanguages, String defaultLanguage, int maxResults) {
        this.languageResolver = new RequestLanguageResolver(supportedLanguages, defaultLanguage);
        this.layerParamValidator = new LayerParamValidator();
        this.maxResults = maxResults;
    }

    public ReverseRequest create(Request webRequest) throws BadRequestException {
        ReverseRequest request = new ReverseRequest();

        for (String queryParam : webRequest.queryParams()) {
            if (!REQUEST_QUERY_PARAMS.contains(queryParam))
                throw new BadRequestException(400, "Unknown query parameter '" + queryParam + "'.  Allowed parameters are: " + REQUEST_QUERY_PARAMS);
        }

        request.setLanguage(languageResolver.resolveRequestedLanguage(webRequest));
        request.setLocation(mandatoryLocationParamConverter.apply(webRequest));

        String radiusParam = webRequest.queryParams("radius");
        if (radiusParam != null) {
            double radius = 1.0;
            try {
                radius = Double.parseDouble(radiusParam);
            } catch (Exception nfe) {
                throw new BadRequestException(400, "Invalid search term 'radius', expected a number.");
            }
            if (radius <= 0) {
                throw new BadRequestException(400, "Invalid search term 'radius', expected a strictly positive number.");
            } else {
                // limit search radius to 5000km
                radius = Math.min(radius, 5000d);
            }
            request.setRadius(radius);
        }

        try {
            request.setLocationDistanceSort(Boolean.parseBoolean(webRequest.queryParamOrDefault("distance_sort", "true")));
        } catch (Exception nfe) {
            throw new BadRequestException(400, "Invalid parameter 'distance_sort', can only be true or false");
        }

        String limitParam = webRequest.queryParams("limit");
        if (limitParam != null) {
            int limit = 1;
            try {
                limit = Integer.parseInt(limitParam);
            } catch (Exception nfe) {
                throw new BadRequestException(400, "Invalid search term 'limit', expected an integer.");
            }
            if (limit <= 0) {
                throw new BadRequestException(400, "Invalid search term 'limit', expected a strictly positive integer.");
            }

            request.setLimit(limit, maxResults);
        }

        request.setDebug(webRequest.queryParams("debug") != null);

        QueryParamsMap layerFiltersQueryMap = webRequest.queryMap("layer");
        if (layerFiltersQueryMap.hasValue()) {
            request.addLayerFilters(layerParamValidator.validate(layerFiltersQueryMap.values()));
        }

        request.setQueryStringFilter(webRequest.queryParams("query_string_filter"));
        request.setReturnGeometry(Boolean.parseBoolean(webRequest.queryParams("geometry")));

        QueryParamsMap tagFiltersQueryMap = webRequest.queryMap("osm_tag");
        if (tagFiltersQueryMap.hasValue()) {
            for (String filter : tagFiltersQueryMap.values()) {
                TagFilter tagFilter = TagFilter.buildOsmTagFilter(filter);
                if (tagFilter == null) {
                    throw new BadRequestException(400, String.format("Invalid parameter 'osm_tag=%s': bad syntax for tag filter.", filter));
                }
                request.addOsmTagFilter(TagFilter.buildOsmTagFilter(filter));
            }
        }

        return request;
    }
}

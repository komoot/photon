package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import spark.QueryParamsMap;
import spark.Request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author svantulden
 */
public class ReverseRequestFactory {
    private final RequestLanguageResolver languageResolver;
    private static final LocationParamConverter mandatoryLocationParamConverter = new LocationParamConverter(true);
    private final ObjectTypeParamValidator objectTypeParamValidator;

    private static final HashSet<String> REQUEST_QUERY_PARAMS = new HashSet<>(Arrays.asList("lang", "lon", "lat", "radius",
            "query_string_filter", "distance_sort", "limit", "object_type"));

    public ReverseRequestFactory(List<String> supportedLanguages, String defaultLanguage) {
        this.languageResolver = new RequestLanguageResolver(supportedLanguages, defaultLanguage);
        this.objectTypeParamValidator = new ObjectTypeParamValidator();
    }

    public ReverseRequest create(Request webRequest) throws BadRequestException {
        for (String queryParam : webRequest.queryParams()) {
            if (!REQUEST_QUERY_PARAMS.contains(queryParam))
                throw new BadRequestException(400, "unknown query parameter '" + queryParam + "'.  Allowed parameters are: " + REQUEST_QUERY_PARAMS);
        }

        String language = languageResolver.resolveRequestedLanguage(webRequest);

        Point location = mandatoryLocationParamConverter.apply(webRequest);

        Double radius = 1d;
        String radiusParam = webRequest.queryParams("radius");
        if (radiusParam != null) {
            try {
                radius = Double.valueOf(radiusParam);
            } catch (Exception nfe) {
                throw new BadRequestException(400, "invalid search term 'radius', expected a number.");
            }
            if (radius <= 0) {
                throw new BadRequestException(400, "invalid search term 'radius', expected a strictly positive number.");
            } else {
                // limit search radius to 5000km
                radius = Math.min(radius, 5000d);
            }
        }

        Boolean locationDistanceSort;
        try {
            locationDistanceSort = Boolean.valueOf(webRequest.queryParamOrDefault("distance_sort", "true"));
        } catch (Exception nfe) {
            throw new BadRequestException(400, "invalid parameter 'distance_sort', can only be true or false");
        }

        Integer limit = 1;
        String limitParam = webRequest.queryParams("limit");
        if (limitParam != null) {
            try {
                limit = Integer.valueOf(limitParam);
            } catch (Exception nfe) {
                throw new BadRequestException(400, "invalid search term 'limit', expected an integer.");
            }
            if (limit <= 0) {
                throw new BadRequestException(400, "invalid search term 'limit', expected a strictly positive integer.");
            } else {
                // limit number of results to 50
                limit = Math.min(limit, 50);
            }
        }

        Set<String> objectTypeFilter = new HashSet<>();
        QueryParamsMap objectTypeFiltersQueryMap = webRequest.queryMap("object_type");
        if (objectTypeFiltersQueryMap.hasValue()) {
            objectTypeFilter = objectTypeParamValidator.validate(objectTypeFiltersQueryMap.values());
        }

        String queryStringFilter = webRequest.queryParams("query_string_filter");
        return new ReverseRequest(location, language, radius, queryStringFilter, limit, locationDistanceSort, objectTypeFilter);
    }
}

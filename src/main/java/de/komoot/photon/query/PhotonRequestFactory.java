package de.komoot.photon.query;

import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.searcher.TagFilter;
import spark.QueryParamsMap;
import spark.Request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A factory that creates a {@link PhotonRequest} from a {@link Request web request}
 */
public class PhotonRequestFactory {
    private final RequestLanguageResolver languageResolver;
    private static final LocationParamConverter optionalLocationParamConverter = new LocationParamConverter(false);
    private final BoundingBoxParamConverter bboxParamConverter;

    private static final HashSet<String> REQUEST_QUERY_PARAMS = new HashSet<>(Arrays.asList("lang", "q", "lon", "lat",
            "limit", "osm_tag", "location_bias_scale", "bbox", "debug", "zoom", "object_type"));

    public PhotonRequestFactory(List<String> supportedLanguages, String defaultLanguage) {
        this.languageResolver = new RequestLanguageResolver(supportedLanguages, defaultLanguage);
        this.bboxParamConverter = new BoundingBoxParamConverter();
    }

    public PhotonRequest create(Request webRequest) throws BadRequestException {
        for (String queryParam : webRequest.queryParams())
            if (!REQUEST_QUERY_PARAMS.contains(queryParam))
                throw new BadRequestException(400, "unknown query parameter '" + queryParam + "'.  Allowed parameters are: " + REQUEST_QUERY_PARAMS);

        String query = webRequest.queryParams("q");
        if (query == null) {
            throw new BadRequestException(400, "missing search term 'q': /?q=berlin");
        }

        PhotonRequest request = new PhotonRequest(query, languageResolver.resolveRequestedLanguage(webRequest));

        request.setLimit(parseInt(webRequest, "limit"));
        request.setLocationForBias(optionalLocationParamConverter.apply(webRequest));
        request.setBbox(bboxParamConverter.apply(webRequest));
        request.setScale(parseDouble(webRequest, "location_bias_scale"));
        request.setZoom(parseInt(webRequest, "zoom"));

        if (webRequest.queryParams("debug") != null) {
            request.enableDebug();
        }

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

        QueryParamsMap objectTypeFiltersQueryMap = webRequest.queryMap("object_type");
        if (objectTypeFiltersQueryMap.hasValue()) {
            List<String> availableObjectTypes = AddressType.getNames();
            for (String objectTypeFilter : objectTypeFiltersQueryMap.values()) {
                if (!availableObjectTypes.contains(objectTypeFilter)) {
                    throw new BadRequestException(
                        400,
                        String.format("Invalid object_type '%s'. Allowed types are: %s", objectTypeFilter, String.join(",", availableObjectTypes))
                    );
                }
                request.addObjectTypeFilter(objectTypeFilter);
            }
        }

        return request;
    }

    private Integer parseInt(Request webRequest, String param) throws BadRequestException {
        Integer intVal = null;
        String value = webRequest.queryParams(param);

        if (value != null && !value.isEmpty()) {
            try {
                intVal = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new BadRequestException(400, String.format("Invalid parameter '%s': must be a number", param));
            }
        }

        return intVal;
    }

    private Double parseDouble(Request webRequest, String param) throws BadRequestException {
        Double outVal = null;
        String value = webRequest.queryParams(param);

        if (value != null && !value.isEmpty()) {
            try {
                outVal = Double.valueOf(value);
            } catch (NumberFormatException e) {
                throw new BadRequestException(400, String.format("Invalid parameter '%s': must be a number", param));
            }

            if (outVal.isNaN()) {
                throw new BadRequestException(400, String.format("Invalid parameter '%s': NaN is not allowed", param));
            }
        }

        return outVal;
    }
}

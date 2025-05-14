package de.komoot.photon.query;

import de.komoot.photon.searcher.TagFilter;
import spark.QueryParamsMap;
import spark.Request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Factory that creates a {@link SimpleSearchRequest} from a {@link Request web request}
 */
public class PhotonRequestFactory {
    private final RequestLanguageResolver languageResolver;
    private static final LocationParamConverter optionalLocationParamConverter = new LocationParamConverter(false);
    private final BoundingBoxParamConverter bboxParamConverter;
    private final LayerParamValidator layerParamValidator;
    private final int maxResults;
    private final boolean supportGeometries;

    private static final HashSet<String> REQUEST_QUERY_PARAMS = new HashSet<>(Arrays.asList("lang", "q", "lon", "lat",
            "limit", "osm_tag", "location_bias_scale", "bbox", "debug", "zoom", "layer", "geometry"));

    private static final HashSet<String> STRUCTURED_ADDRESS_FIELDS = new HashSet<>(Arrays.asList("countrycode", "state", "county", "city",
            "postcode", "district", "housenumber", "street"));
    private static final HashSet<String> STRUCTURED_REQUEST_QUERY_PARAMS = new HashSet<>(Arrays.asList("lang", "limit",
            "lon", "lat", "osm_tag", "location_bias_scale", "bbox", "debug", "zoom", "layer",
            "countrycode", "state", "county", "city", "postcode", "district", "housenumber", "street"));


    public PhotonRequestFactory(List<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        this.languageResolver = new RequestLanguageResolver(supportedLanguages, defaultLanguage);
        this.bboxParamConverter = new BoundingBoxParamConverter();
        this.layerParamValidator = new LayerParamValidator();
        this.maxResults = maxResults;
        this.supportGeometries = supportGeometries;
    }

    public StructuredSearchRequest createStructured(Request webRequest) throws BadRequestException {
        boolean hasAddressQueryParam = false;
        for (String queryParam : webRequest.queryParams()) {
            if (!STRUCTURED_REQUEST_QUERY_PARAMS.contains(queryParam))
                throw new BadRequestException(400, "unknown query parameter '" + queryParam + "'.  Allowed parameters are: " + STRUCTURED_REQUEST_QUERY_PARAMS);

            if (STRUCTURED_ADDRESS_FIELDS.contains(queryParam)) {
                hasAddressQueryParam = true;
            }
        }

        if (!hasAddressQueryParam)
            throw new BadRequestException(400, "at least one of the parameters " + STRUCTURED_ADDRESS_FIELDS + " is required.");

        StructuredSearchRequest result = new StructuredSearchRequest(languageResolver.resolveRequestedLanguage(webRequest));
        result.setCountryCode(webRequest.queryParams("countrycode"));
        result.setState(webRequest.queryParams("state"));
        result.setCounty(webRequest.queryParams("county"));
        result.setCity(webRequest.queryParams("city"));
        result.setPostCode(webRequest.queryParams("postcode"));
        result.setDistrict(webRequest.queryParams("district"));
        result.setStreet(webRequest.queryParams("street"));
        result.setHouseNumber(webRequest.queryParams("housenumber"));


        addCommonParameters(webRequest, result);

        return result;
    }

    public SimpleSearchRequest create(Request webRequest) throws BadRequestException {
        for (String queryParam : webRequest.queryParams())
            if (!REQUEST_QUERY_PARAMS.contains(queryParam))
                throw new BadRequestException(400, "unknown query parameter '" + queryParam + "'.  Allowed parameters are: " + REQUEST_QUERY_PARAMS);

        String query = webRequest.queryParams("q");
        if (query == null) {
            throw new BadRequestException(400, "missing search term 'q': /?q=berlin");
        }

        SimpleSearchRequest request = new SimpleSearchRequest(query, languageResolver.resolveRequestedLanguage(webRequest));

        addCommonParameters(webRequest, request);

        return request;
    }

    private void addCommonParameters(Request webRequest, SearchRequestBase request) throws BadRequestException {
        Integer limit = parseInt(webRequest, "limit");
        if (limit != null) {
            request.setLimit(Integer.max(Integer.min(limit, maxResults), 1));
        }
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

        QueryParamsMap layerFiltersQueryMap = webRequest.queryMap("layer");
        if (layerFiltersQueryMap.hasValue()) {
            request.setLayerFilter(layerParamValidator.validate(layerFiltersQueryMap.values()));
        }

        // If the database supports geometries, return them by default.
        request.setReturnGeometry(supportGeometries);

        // Check if the user explicitly doesn't want a geometry.
        if (webRequest.queryParams("geometry") != null) {
            request.setReturnGeometry(parseBoolean(webRequest, "geometry"));
        }
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

    private Boolean parseBoolean(Request webRequest, String param) {
        boolean booleanVal = false;
        String value = webRequest.queryParams(param);

        if (value != null && !value.isEmpty()) {
            booleanVal = Boolean.parseBoolean(value);
        }

        return booleanVal;
    }
}

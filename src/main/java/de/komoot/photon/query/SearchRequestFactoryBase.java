package de.komoot.photon.query;

import de.komoot.photon.searcher.TagFilter;
import spark.QueryParamsMap;
import spark.Request;

import java.util.List;

/**
 * Factory that creates a {@link SimpleSearchRequest} from a {@link Request web request}
 */
public class SearchRequestFactoryBase {
    private final RequestLanguageResolver languageResolver;
    private static final LocationParamConverter optionalLocationParamConverter = new LocationParamConverter(false);
    private final BoundingBoxParamConverter bboxParamConverter;
    private final LayerParamValidator layerParamValidator;
    private final int maxResults;
    private final boolean supportGeometries;

    protected SearchRequestFactoryBase(List<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        this.languageResolver = new RequestLanguageResolver(supportedLanguages, defaultLanguage);
        this.bboxParamConverter = new BoundingBoxParamConverter();
        this.layerParamValidator = new LayerParamValidator();
        this.maxResults = maxResults;
        this.supportGeometries = supportGeometries;
    }

    protected void addCommonParameters(Request webRequest, SearchRequestBase request) throws BadRequestException {
        request.setLanguage(languageResolver.resolveRequestedLanguage(webRequest));
        request.setLimit(parseInt(webRequest, "limit"), maxResults);
        request.setLocationForBias(optionalLocationParamConverter.apply(webRequest));
        request.setBbox(bboxParamConverter.apply(webRequest));
        request.setScale(parseDouble(webRequest, "location_bias_scale"));
        request.setZoom(parseInt(webRequest, "zoom"));

        if (webRequest.queryParams("debug") != null) {
            request.setDebug(true);
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
            request.addLayerFilters(layerParamValidator.validate(layerFiltersQueryMap.values()));
        }

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

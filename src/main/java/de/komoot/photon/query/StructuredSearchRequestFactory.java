package de.komoot.photon.query;

import spark.Request;

import java.util.List;
import java.util.Set;

public class StructuredSearchRequestFactory extends SearchRequestFactoryBase {
    private static final Set<String> STRUCTURED_ADDRESS_FIELDS = Set.of("countrycode", "state", "county", "city",
            "postcode", "district", "housenumber", "street");
    private static final Set<String> STRUCTURED_REQUEST_QUERY_PARAMS = Set.of("lang", "limit",
            "lon", "lat", "osm_tag", "location_bias_scale", "bbox", "debug", "zoom", "layer",
            "countrycode", "state", "county", "city", "postcode", "district", "housenumber", "street");



    public StructuredSearchRequestFactory(List<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        super(supportedLanguages, defaultLanguage, maxResults, supportGeometries);
    }

    public StructuredSearchRequest create(Request webRequest) throws BadRequestException {
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

        StructuredSearchRequest result = new StructuredSearchRequest();
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

}

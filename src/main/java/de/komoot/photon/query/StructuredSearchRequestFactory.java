package de.komoot.photon.query;

import io.javalin.http.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NullMarked
public class StructuredSearchRequestFactory extends SearchRequestFactoryBase implements RequestFactory<StructuredSearchRequest> {
    private static final List<String> STRUCTURED_ADDRESS_FIELDS = List.of(
            "countrycode", "state", "county", "city",
            "postcode", "district", "housenumber", "street");
    private static final Set<String> STRUCTURED_SEARCH_PARAMETERS =
            Stream.concat(SEARCH_PARAMETERS.stream(), STRUCTURED_ADDRESS_FIELDS.stream())
                    .collect(Collectors.toSet());


    public StructuredSearchRequestFactory(Set<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        super(supportedLanguages, defaultLanguage, maxResults, supportGeometries);
    }

    public StructuredSearchRequest create(Context context) {
        checkParams(context, STRUCTURED_SEARCH_PARAMETERS);

        if (STRUCTURED_ADDRESS_FIELDS.stream()
                .noneMatch(s -> context.queryParam(s) != null)) {
            throw new BadRequestException(400, "at least one of the parameters "
                    + STRUCTURED_ADDRESS_FIELDS + " is required.");
        }

        final var request = new StructuredSearchRequest();

        completeSearchRequest(request, context);
        request.setCountryCode(context.queryParam("countrycode"));
        request.setState(context.queryParam("state"));
        request.setCounty(context.queryParam("county"));
        request.setCity(context.queryParam("city"));
        request.setPostCode(context.queryParam("postcode"));
        request.setDistrict(context.queryParam("district"));
        request.setStreet(context.queryParam("street"));
        request.setHouseNumber(context.queryParam("housenumber"));

        return request;
    }

}

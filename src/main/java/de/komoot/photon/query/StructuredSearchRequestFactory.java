package de.komoot.photon.query;

import de.komoot.photon.analyzer.normalization.Normalizer;
import io.javalin.http.Context;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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


    public StructuredSearchRequestFactory(Set<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries, Normalizer normalizer) {
        super(supportedLanguages, defaultLanguage, maxResults, supportGeometries, normalizer);
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
        request.setCountryCode(getNormalizedParam(context, "countrycode"));
        request.setState(getNormalizedParam(context, "state"));
        request.setCounty(getNormalizedParam(context, "county"));
        request.setCity(getNormalizedParam(context, "city"));
        request.setPostCode(getNormalizedParam(context, "postcode"));
        request.setDistrict(getNormalizedParam(context, "district"));
        request.setStreet(getNormalizedParam(context, "street"));
        request.setHouseNumber(getNormalizedParam(context, "housenumber"));

        return request;
    }

    @Nullable
    private String getNormalizedParam(Context context, String name) {
        var param = context.queryParam(name);
        return param == null ? null : normalizer.transform(param);
    }

}

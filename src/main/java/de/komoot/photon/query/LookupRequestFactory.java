package de.komoot.photon.query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import spark.Request;

/**
 * A factory that creates a {@link LookupRequest} from a {@link Request web request}
 */
public class LookupRequestFactory {
    protected static HashSet<String> allowedQueryParams = new HashSet<>(Arrays.asList("lang", "placeId"));
    private final RequestLanguageResolver languageResolver;

    public LookupRequestFactory(List<String> supportedLanguages, String defaultLanguage) {
        this.languageResolver = new RequestLanguageResolver(supportedLanguages, defaultLanguage);
    }

    public LookupRequest create(Request webRequest) throws BadRequestException {
        String language = this.languageResolver.resolveRequestedLanguage(webRequest);
        String placeId = webRequest.queryParams("placeId");

        if (placeId != null) {
            validateQueryParams(webRequest);
            return new LookupRequest(placeId, language);
        } else {
            throw new BadRequestException(400, "Missing required query parameter 'placeId'");
        }
    }

    private void validateQueryParams(Request webRequest) throws BadRequestException {
        HashSet<String> params = new HashSet<>(webRequest.queryParams());

        params.removeAll(allowedQueryParams);

        if (!params.isEmpty()) {
            throw new BadRequestException(
                    400,
                    "Unknown query parameter(s): " + params + ". Allowed parameters are: " + allowedQueryParams
            );
        }
    }

}

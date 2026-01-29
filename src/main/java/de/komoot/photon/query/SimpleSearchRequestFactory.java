package de.komoot.photon.query;

import io.javalin.http.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NullMarked
public class SimpleSearchRequestFactory extends SearchRequestFactoryBase implements RequestFactory<SimpleSearchRequest> {
    private static final Set<String> FREE_SEARCH_PARAMETERS =
            Stream.concat(SEARCH_PARAMETERS.stream(), Stream.of("q"))
                    .collect(Collectors.toSet());

    public SimpleSearchRequestFactory(List<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        super(supportedLanguages, defaultLanguage, maxResults, supportGeometries);
    }

    public SimpleSearchRequest create(Context context) {
        checkParams(context, FREE_SEARCH_PARAMETERS);

        final var request = new SimpleSearchRequest();
        completeSearchRequest(request, context);

        var query = context.queryParamAsClass("q", String.class).getOrDefault("");
        if (query.isBlank()) {
            if (request.getIncludeCategories().isEmpty()) {
                throw new BadRequestException(400, "q parameter is required when no include categories are specified");
            }
        } else {
            request.setQuery(query.strip());
        }

        return request;
    }
}

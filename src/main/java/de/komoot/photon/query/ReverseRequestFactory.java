package de.komoot.photon.query;

import io.javalin.http.Context;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReverseRequestFactory extends RequestFactoryBase implements RequestFactory<ReverseRequest> {
    private static final Set<String> REVERSE_PARAMETERS =
            Stream.concat(BASE_PARAMETERS.stream(),
                            Stream.of("lat", "lon", "radius", "query_string_filter", "distance_sort"))
                    .collect(Collectors.toSet());

    public ReverseRequestFactory(List<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        super(supportedLanguages, defaultLanguage, maxResults, supportGeometries);
    }

    public ReverseRequest create(Context context) {
        checkParams(context, REVERSE_PARAMETERS);

        final var request = new ReverseRequest();

        completeBaseRequest(request, context);
        request.setLocation(parseLatLon(context, true));
        request.setRadius(context.queryParamAsClass("radius", Double.class).allowNullable().get());
        request.setQueryStringFilter(context.queryParam("query_string_filter"));
        request.setLocationDistanceSort(context.queryParamAsClass("distance_sort", Boolean.class).getOrDefault(true));

        return request;
    }
}

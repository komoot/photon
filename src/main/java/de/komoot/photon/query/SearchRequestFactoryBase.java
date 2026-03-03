package de.komoot.photon.query;

import io.javalin.http.Context;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Envelope;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NullMarked
public class SearchRequestFactoryBase extends RequestFactoryBase {
    protected static final Set<String> SEARCH_PARAMETERS =
            Stream.concat(BASE_PARAMETERS.stream(),
                            Stream.of("lat", "lon", "location_bias_scale", "zoom", "bbox", "suggest_addresses"))
                    .collect(Collectors.toSet());

    protected SearchRequestFactoryBase(Set<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        super(supportedLanguages, defaultLanguage, maxResults, supportGeometries);
    }

    protected void completeSearchRequest(SearchRequestBase request, Context context) {
        completeBaseRequest(request, context);

        request.setZoom(context.queryParamAsClass("zoom", Integer.class)
                .getOrNull());

        request.setScale(context.queryParamAsClass("location_bias_scale", Double.class)
                .getOrNull());

        request.setLocationForBias(parseLatLon(context, false));
        request.setBbox(context.queryParamAsClass("bbox", Envelope.class)
                .getOrNull());
        request.setSuggestAddresses(context.queryParamAsClass("suggest_addresses", Boolean.class)
                .getOrDefault(false));
    }

}

package de.komoot.photon.query;

import io.javalin.http.Context;
import org.locationtech.jts.geom.Envelope;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchRequestFactoryBase extends RequestFactoryBase {
    protected static final Set<String> SEARCH_PARAMETERS =
            Stream.concat(BASE_PARAMETERS.stream(),
                            Stream.of("lat", "lon", "location_bias_scale", "zoom", "bbox", "include_housenumbers"))
                    .collect(Collectors.toSet());

    protected SearchRequestFactoryBase(List<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        super(supportedLanguages, defaultLanguage, maxResults, supportGeometries);
    }

    protected void completeSearchRequest(SearchRequestBase request, Context context) {
        completeBaseRequest(request, context);

        request.setZoom(context.queryParamAsClass("zoom", Integer.class)
                .allowNullable().get());

        request.setScale(context.queryParamAsClass("location_bias_scale", Double.class)
                .allowNullable()
                .get());

        request.setLocationForBias(parseLatLon(context, false));
        request.setBbox(context.queryParamAsClass("bbox", Envelope.class)
                .allowNullable().get());
        request.setIncludeHousenumbers(context.queryParamAsClass("include_housenumbers", Boolean.class)
                .getOrDefault(false));
    }

}

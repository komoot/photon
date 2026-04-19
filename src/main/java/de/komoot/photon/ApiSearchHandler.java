package de.komoot.photon;

import de.komoot.photon.openapi.PhotonFeatureCollection;
import de.komoot.photon.query.SimpleSearchRequest;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jspecify.annotations.NullMarked;

/**
 * Handler for the /api (forward geocoding) endpoint, documented for OpenAPI.
 */
@NullMarked
public class ApiSearchHandler implements Handler {
    private final GenericSearchHandler<SimpleSearchRequest> delegate;

    public ApiSearchHandler(GenericSearchHandler<SimpleSearchRequest> delegate) {
        this.delegate = delegate;
    }

    @OpenApi(
        path = "/api",
        methods = {HttpMethod.GET},
        summary = "Forward geocoding",
        description = "Search for places by name or address. Returns a GeoJSON FeatureCollection.",
        tags = {"Geocoding"},
        queryParams = {
            @OpenApiParam(name = "q", description = "Free-form search query. Required unless 'include' categories are specified.", required = false),
            @OpenApiParam(name = "lang", description = "Preferred language for result labels (ISO 639-1 code, e.g. 'en', 'de'). Falls back to Accept-Language header, then the server default."),
            @OpenApiParam(name = "limit", description = "Maximum number of results to return.", type = Integer.class),
            @OpenApiParam(name = "lat", description = "Latitude for location bias (WGS 84).", type = Double.class),
            @OpenApiParam(name = "lon", description = "Longitude for location bias (WGS 84).", type = Double.class),
            @OpenApiParam(name = "zoom", description = "Map zoom level (0–18) used to tune location bias.", type = Integer.class),
            @OpenApiParam(name = "location_bias_scale", description = "Scale factor for the location bias (0.0–1.0). Higher values give more weight to proximity.", type = Double.class),
            @OpenApiParam(name = "bbox", description = "Bounding-box filter as 'minLon,minLat,maxLon,maxLat'. Only results inside the box are returned."),
            @OpenApiParam(name = "countrycode", description = "Comma-separated ISO 3166-1 alpha-2 country codes to restrict results (e.g. 'de,at')."),
            @OpenApiParam(name = "osm_tag", description = "OSM tag filter. Prefix with ':' to include, prefix with '!' to exclude. Can be repeated."),
            @OpenApiParam(name = "layer", description = "Layer filter (e.g. 'house', 'street', 'city'). Can be repeated."),
            @OpenApiParam(name = "include", description = "Category include filter (e.g. 'accommodation'). Can be repeated."),
            @OpenApiParam(name = "exclude", description = "Category exclude filter. Can be repeated."),
            @OpenApiParam(name = "dedupe", description = "Remove near-duplicate results (default: true).", type = Boolean.class),
            @OpenApiParam(name = "geometry", description = "Include the full geometry in the response (default: false).", type = Boolean.class),
            @OpenApiParam(name = "suggest_addresses", description = "Bias results towards address matches (default: false).", type = Boolean.class),
            @OpenApiParam(name = "debug", description = "Include debug information in the response (default: false).", type = Boolean.class)
        },
        responses = {
            @OpenApiResponse(status = "200", description = "GeoJSON FeatureCollection with geocoding results.",
                content = @OpenApiContent(from = PhotonFeatureCollection.class, type = "application/json")),
            @OpenApiResponse(status = "400", description = "Bad request — invalid or missing query parameters.")
        }
    )
    @Override
    public void handle(Context ctx) throws Exception {
        delegate.handle(ctx);
    }
}

package de.komoot.photon;

import de.komoot.photon.openapi.PhotonFeatureCollection;
import de.komoot.photon.query.ReverseRequest;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jspecify.annotations.NullMarked;

/**
 * Handler for the /reverse (reverse geocoding) endpoint, documented for OpenAPI.
 */
@NullMarked
public class ReverseSearchHandler implements Handler {
    private final GenericSearchHandler<ReverseRequest> delegate;

    public ReverseSearchHandler(GenericSearchHandler<ReverseRequest> delegate) {
        this.delegate = delegate;
    }

    @OpenApi(
        path = "/reverse",
        methods = {HttpMethod.GET},
        summary = "Reverse geocoding",
        description = "Look up the nearest address or place for a given coordinate. Returns a GeoJSON FeatureCollection.",
        tags = {"Geocoding"},
        queryParams = {
            @OpenApiParam(name = "lat", description = "Latitude of the query point (WGS 84, -90 to 90).", required = true, type = Double.class),
            @OpenApiParam(name = "lon", description = "Longitude of the query point (WGS 84, -180 to 180).", required = true, type = Double.class),
            @OpenApiParam(name = "radius", description = "Maximum search radius in kilometres.", type = Double.class),
            @OpenApiParam(name = "distance_sort", description = "Sort results by distance from the query point (default: true).", type = Boolean.class),
            @OpenApiParam(name = "query_string_filter", description = "Additional OpenSearch query filter applied to the results."),
            @OpenApiParam(name = "lang", description = "Preferred language for result labels (ISO 639-1 code). Falls back to Accept-Language header, then the server default."),
            @OpenApiParam(name = "limit", description = "Maximum number of results to return.", type = Integer.class),
            @OpenApiParam(name = "osm_tag", description = "OSM tag filter. Prefix with ':' to include, prefix with '!' to exclude. Can be repeated."),
            @OpenApiParam(name = "layer", description = "Layer filter (e.g. 'house', 'street', 'city'). Can be repeated."),
            @OpenApiParam(name = "include", description = "Category include filter. Can be repeated."),
            @OpenApiParam(name = "exclude", description = "Category exclude filter. Can be repeated."),
            @OpenApiParam(name = "dedupe", description = "Remove near-duplicate results (default: true).", type = Boolean.class),
            @OpenApiParam(name = "geometry", description = "Include the full geometry in the response (default: false).", type = Boolean.class),
            @OpenApiParam(name = "debug", description = "Include debug information in the response (default: false).", type = Boolean.class)
        },
        responses = {
            @OpenApiResponse(status = "200", description = "GeoJSON FeatureCollection with reverse geocoding results.",
                content = @OpenApiContent(from = PhotonFeatureCollection.class, type = "application/json")),
            @OpenApiResponse(status = "400", description = "Bad request — invalid or missing query parameters.")
        }
    )
    @Override
    public void handle(Context ctx) throws Exception {
        delegate.handle(ctx);
    }
}

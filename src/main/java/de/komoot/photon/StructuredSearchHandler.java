package de.komoot.photon;

import de.komoot.photon.query.StructuredSearchRequest;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jspecify.annotations.NullMarked;

/**
 * Handler for the /structured (structured forward geocoding) endpoint, documented for OpenAPI.
 */
@NullMarked
public class StructuredSearchHandler implements Handler {
    private final GenericSearchHandler<StructuredSearchRequest> delegate;

    public StructuredSearchHandler(GenericSearchHandler<StructuredSearchRequest> delegate) {
        this.delegate = delegate;
    }

    @OpenApi(
        path = "/structured",
        methods = {HttpMethod.GET},
        summary = "Structured forward geocoding",
        description = "Search for places using individual address components. At least one address field is required. Returns a GeoJSON FeatureCollection.",
        tags = {"Geocoding"},
        queryParams = {
            @OpenApiParam(name = "countrycode", description = "ISO 3166-1 alpha-2 country code (e.g. 'de')."),
            @OpenApiParam(name = "state", description = "State or region name."),
            @OpenApiParam(name = "county", description = "County name."),
            @OpenApiParam(name = "city", description = "City name."),
            @OpenApiParam(name = "postcode", description = "Postal code."),
            @OpenApiParam(name = "district", description = "District or suburb name."),
            @OpenApiParam(name = "street", description = "Street name."),
            @OpenApiParam(name = "housenumber", description = "House number."),
            @OpenApiParam(name = "lang", description = "Preferred language for result labels (ISO 639-1 code). Falls back to Accept-Language header, then the server default."),
            @OpenApiParam(name = "limit", description = "Maximum number of results to return.", type = Integer.class),
            @OpenApiParam(name = "lat", description = "Latitude for location bias (WGS 84).", type = Double.class),
            @OpenApiParam(name = "lon", description = "Longitude for location bias (WGS 84).", type = Double.class),
            @OpenApiParam(name = "zoom", description = "Map zoom level (0–18) used to tune location bias.", type = Integer.class),
            @OpenApiParam(name = "location_bias_scale", description = "Scale factor for the location bias (0.0–1.0).", type = Double.class),
            @OpenApiParam(name = "bbox", description = "Bounding-box filter as 'minLon,minLat,maxLon,maxLat'."),
            @OpenApiParam(name = "osm_tag", description = "OSM tag filter. Prefix with ':' to include, prefix with '!' to exclude. Can be repeated."),
            @OpenApiParam(name = "layer", description = "Layer filter (e.g. 'house', 'street', 'city'). Can be repeated."),
            @OpenApiParam(name = "include", description = "Category include filter. Can be repeated."),
            @OpenApiParam(name = "exclude", description = "Category exclude filter. Can be repeated."),
            @OpenApiParam(name = "dedupe", description = "Remove near-duplicate results (default: true).", type = Boolean.class),
            @OpenApiParam(name = "geometry", description = "Include the full geometry in the response (default: false).", type = Boolean.class),
            @OpenApiParam(name = "suggest_addresses", description = "Bias results towards address matches (default: false).", type = Boolean.class),
            @OpenApiParam(name = "debug", description = "Include debug information in the response (default: false).", type = Boolean.class)
        },
        responses = {
            @OpenApiResponse(status = "200", description = "GeoJSON FeatureCollection with geocoding results.",
                content = @OpenApiContent(type = "application/json")),
            @OpenApiResponse(status = "400", description = "Bad request — invalid or missing query parameters.")
        }
    )
    @Override
    public void handle(Context ctx) throws Exception {
        delegate.handle(ctx);
    }
}

package de.komoot.photon.query;

import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.searcher.TagFilter;
import io.javalin.http.Context;
import io.javalin.http.Header;
import org.locationtech.jts.geom.*;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RequestFactoryBase {
    protected static final Set<String> BASE_PARAMETERS = Set.of(
        "lang", "limit", "debug", "dedupe", "geometry", "osm_tag", "layer");
    private static final List<String> AVAILABLE_LAYERS = AddressType.getNames();
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final List<String> supportedLanguages;
    private final String defaultLangauge;
    private final int maxResults;
    private final boolean supportGeometries;

    protected RequestFactoryBase(List<String> supportedLanguages, String defaultLanguage, int maxResults, boolean supportGeometries) {
        this.supportedLanguages = supportedLanguages;
        this.defaultLangauge = defaultLanguage;
        this.maxResults = maxResults;
        this.supportGeometries = supportGeometries;
    }

    protected void completeBaseRequest(RequestBase request, Context context) {
        request.setLanguage(parseLanguage(context));

        request.setLimit(context.queryParamAsClass("limit", Integer.class)
                                .allowNullable()
                                .get(), maxResults);

        request.setDebug(context.queryParamAsClass("debug", Boolean.class).getOrDefault(false));

        request.setDedupe(context.queryParamAsClass("dedupe", Boolean.class).getOrDefault(true));

        request.addLayerFilters(context.queryParamsAsClass("layer", String.class)
                .allowNullable()
                .check(AVAILABLE_LAYERS::containsAll,
                        "Unknown layer type. Available layers: " + AVAILABLE_LAYERS)
                .get());

        final var tagFilters = context.queryParamsAsClass("osm_tag", TagFilter.class)
                .allowNullable().get();

        if (tagFilters != null) {
            for (var filter: tagFilters) {
                if (filter != null) {
                    request.addOsmTagFilter(filter);
                }
            }
        }

        request.setReturnGeometry(context.queryParamAsClass("geometry", Boolean.class)
                        .check(g -> !g || supportGeometries,
                                "Geometry output requested but not available in database.")
                        .getOrDefault(false));
    }

    private String parseLanguage(Context context) {
        final String langParam = context.queryParamAsClass("lang", String.class)
                .allowNullable()
                .check(l -> l == null || "default".equals(l) || supportedLanguages.contains(l),
                        "Language is not supported. Supported are: default, "
                                + String.join(", ", supportedLanguages))
                .get();

        if (langParam != null) {
            return langParam;
        }

        // no parameter, try the language header
        final String langHeader = context.header(Header.ACCEPT_LANGUAGE);
        if (langHeader != null && !langHeader.isBlank()) {
            try {
                var languages = Locale.LanguageRange.parse(langHeader);
                return Locale.lookupTag(languages, supportedLanguages);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        // finally fall back to the default
        return defaultLangauge;
    }

    protected Point parseLatLon(Context context, boolean mandatory) {
        Double lat, lon;
        if (mandatory) {
            lat = context.queryParamAsClass("lat", Double.class)
                    .check(l -> l >= -90.0 && l <= 90.0,
                            "Invalid value for 'lat' parameter.")
                    .get();

            lon = context.queryParamAsClass("lon", Double.class)
                    .check(l -> l >= -180.0 && l <= 180.0,
                            "Invalid value for 'lon' parameter.")
                    .get();
        } else {
            lat = context.queryParamAsClass("lat", Double.class)
                    .allowNullable()
                    .check(l -> l == null || (l >= -90.0 && l <= 90.0),
                            "Invalid value for 'lat' parameter.")
                    .check(l -> l == null || context.queryParam("lon") != null,
                            "Missing parameter 'lon'.")
                    .get();

            lon = context.queryParamAsClass("lon", Double.class)
                    .allowNullable()
                    .check(l -> l == null || (l >= -180.0 && l <= 180.0),
                            "Invalid value for 'lon' parameter.")
                    .check(l -> l == null || lat != null,
                            "Missing parameter 'lat'.")
                    .get();
        }

        if (lat == null || lon == null) {
            return null;
        }

        return GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
    }

    protected void checkParams(Context context, Set<String> allowedParameters) {
        for (var param : context.queryParamMap().keySet()) {
            if (!allowedParameters.contains(param)) {
                throw new BadRequestException(400, "Unknown query parameter '" + param
                        + "'.  Allowed parameters are: " + allowedParameters);
            }
        }

    }
}

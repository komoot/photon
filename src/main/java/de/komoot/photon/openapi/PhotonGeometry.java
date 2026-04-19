package de.komoot.photon.openapi;

import io.javalin.openapi.OpenApiByFields;
import io.javalin.openapi.OpenApiNullable;
import io.javalin.openapi.Visibility;

/**
 * GeoJSON geometry of a Photon result.
 * By default this is always a Point. When the geometry=true query parameter is set,
 * this may be any GeoJSON geometry type (Polygon, LineString, etc.) for results
 * that have a full geometry stored.
 */
@OpenApiByFields(Visibility.PUBLIC)
public class PhotonGeometry {
    /** GeoJSON geometry type, e.g. "Point", "Polygon", "LineString". */
    public String type;

    /**
     * Coordinates in WGS 84. For Point: [longitude, latitude].
     * For other geometry types the structure follows the GeoJSON spec (RFC 7946).
     */
    @OpenApiNullable
    public double[] coordinates;
}

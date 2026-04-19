package de.komoot.photon.openapi;

import io.javalin.openapi.OpenApiByFields;
import io.javalin.openapi.OpenApiNullable;
import io.javalin.openapi.Visibility;

import java.util.Map;

/**
 * Properties of a Photon geocoding GeoJSON Feature.
 * All address and classification fields are optional and only present when available.
 */
@OpenApiByFields(Visibility.PUBLIC)
public class PhotonProperties {
    /** OSM element type: "N" (node), "W" (way), or "R" (relation). */
    @OpenApiNullable
    public String osm_type;

    /** OSM element ID. */
    @OpenApiNullable
    public Long osm_id;

    /** Primary OSM tag key (e.g. "place", "highway", "amenity"). */
    @OpenApiNullable
    public String osm_key;

    /** Primary OSM tag value (e.g. "city", "residential", "restaurant"). */
    @OpenApiNullable
    public String osm_value;

    /** Photon place type (e.g. "house", "street", "city", "country"). */
    @OpenApiNullable
    public String type;

    /** Primary display name in the requested language. */
    @OpenApiNullable
    public String name;

    /** House number. */
    @OpenApiNullable
    public String housenumber;

    /** Street name in the requested language. */
    @OpenApiNullable
    public String street;

    /** Locality or neighbourhood name. */
    @OpenApiNullable
    public String locality;

    /** District or suburb name. */
    @OpenApiNullable
    public String district;

    /** City name. */
    @OpenApiNullable
    public String city;

    /** County name. */
    @OpenApiNullable
    public String county;

    /** State or region name. */
    @OpenApiNullable
    public String state;

    /** Country name. */
    @OpenApiNullable
    public String country;

    /** ISO 3166-1 alpha-2 country code (lowercase, e.g. "de"). */
    @OpenApiNullable
    public String countrycode;

    /** Postal code. */
    @OpenApiNullable
    public String postcode;

    /** Bounding box as [minLon, minLat, maxLon, maxLat]. */
    @OpenApiNullable
    public double[] extent;

    /** Extra OSM tags imported during indexing. */
    @OpenApiNullable
    public Map<String, String> extra;
}

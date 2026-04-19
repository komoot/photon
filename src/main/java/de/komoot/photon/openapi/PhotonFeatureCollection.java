package de.komoot.photon.openapi;

import io.javalin.openapi.OpenApiByFields;
import io.javalin.openapi.Visibility;

/**
 * GeoJSON FeatureCollection returned by all Photon geocoding endpoints.
 */
@OpenApiByFields(Visibility.PUBLIC)
public class PhotonFeatureCollection {
    /** Always "FeatureCollection". */
    public String type;

    /** List of geocoding result features. */
    public PhotonFeature[] features;
}

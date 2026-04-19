package de.komoot.photon.openapi;

import io.javalin.openapi.OpenApiByFields;
import io.javalin.openapi.Visibility;

/**
 * A single GeoJSON Feature returned by Photon.
 */
@OpenApiByFields(Visibility.PUBLIC)
public class PhotonFeature {
    /** Always "Feature". */
    public String type;

    /** Location and shape of the result. */
    public PhotonGeometry geometry;

    /** Address and classification data. */
    public PhotonProperties properties;
}

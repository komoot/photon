package de.komoot.photon.searcher;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Interface describing a single response object from the database.
 */
@NullMarked
public interface PhotonResult {
    double[] INVALID_COORDINATES = new double[]{0, 0};

    @Nullable
    Object get(String key);

    @Nullable
    String getLocalised(String key, String language);

    @Nullable
    Map<String, String> getMap(String key);

    double[] getCoordinates();

    @Nullable
    String getGeometry();

    double @Nullable [] getExtent();

    Map<String, Object> getRawData();
}

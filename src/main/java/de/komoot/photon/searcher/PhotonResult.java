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
    String getLocalised(String key, String language, String... altNames);

    double[] getCoordinates();

    double @Nullable [] getExtent();

    double getScore();

    void adjustScore(double difference);

    Map<String, Object> getRawData();
}

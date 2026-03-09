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

    default String getOrDefault(String key, String defValue) {
        String value = (String) get(key);
        return value == null ? defValue : value;
    }

    @Nullable
    String getLocalised(String key, String language, String... altNames);

    double[] getCoordinates();

    double @Nullable [] getExtent();

    double getScore();

    void adjustScore(double difference);

    Map<String, Object> getRawData();
}

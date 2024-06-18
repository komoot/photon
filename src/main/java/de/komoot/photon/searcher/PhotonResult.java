package de.komoot.photon.searcher;

import java.util.Map;

/**
 * Interface describing a single response object from the database.
 */
public interface PhotonResult {
    final double[] INVALID_COORDINATES = new double[]{0, 0};

    /**
     * Get the value for the given field.
     *
     * Should throw an exception when the field has multiple values.
     *
     * @param key
     * @return If the field exist, the string value of the field, else null.
     */
    Object get(String key);

    String getLocalised(String key, String language);
    Map<String, String> getMap(String key);

    double[] getCoordinates();
    
    double[] getExtent();

    double getScore();

}

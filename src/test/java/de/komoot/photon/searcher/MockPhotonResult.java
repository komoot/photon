package de.komoot.photon.searcher;

import java.util.HashMap;
import java.util.Map;

public class MockPhotonResult implements PhotonResult {

    final Map<String, Object> data = new HashMap<>();
    final double[] coordinates = new double[]{42, 21};
    final String geometry = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-100.0,40.0],[-100.0,45.0],[-90.0,45.0],[-90.0,40.0],[-100.0,40.0]]],[[[-80.0,35.0],[-80.0,40.0],[-70.0,40.0],[-70.0,35.0],[-80.0,35.0]]]]}";
    final double[] extent = new double[]{0, 1, 2, 3};
    final Map<String, String> localized = new HashMap<>();

    @Override
    public Object get(String key) {
        return data.getOrDefault(key, null);
    }

    @Override
    public String getLocalised(String key, String language) {
        return localized.getOrDefault(key + "||" + language, null);
    }

    @Override
    public Map<String, String> getMap(String key) {
        return (Map<String, String>) data.getOrDefault(key, null);
    }

    @Override
    public double[] getCoordinates() {
        return coordinates;
    }

    @Override
    public String getGeometry() {
        return geometry;
    }

    @Override
    public double[] getExtent() {
        return extent;
    }

    @Override
    public double getScore() {
        return 99;
    }

    public MockPhotonResult put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public MockPhotonResult putLocalized(String key, String lang, String value) {
        localized.put(key + "||" + lang, value);
        return this;
    }

    @Override
    public Map<String, Object> getRawData() {
        return Map.of();
    }
}

package de.komoot.photon.searcher;

import java.util.HashMap;
import java.util.Map;

public class MockPhotonResult implements PhotonResult {

    final Map<String, Object> data = new HashMap<>();
    final double[] coordinates = new double[]{42, 21};
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
}

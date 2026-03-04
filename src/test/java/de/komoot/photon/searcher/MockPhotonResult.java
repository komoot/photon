package de.komoot.photon.searcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@NullMarked
public class MockPhotonResult implements PhotonResult {

    final Map<String, Object> data = new HashMap<>();
    final double[] coordinates = new double[]{42, 21};
    String geometry = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-100.0,40.0],[-100.0,45.0],[-90.0,45.0],[-90.0,40.0],[-100.0,40.0]]],[[[-80.0,35.0],[-80.0,40.0],[-70.0,40.0],[-70.0,35.0],[-80.0,35.0]]]]}";
    final double[] extent = new double[]{0, 1, 2, 3};
    final Map<String, String> localized = new HashMap<>();
    double score = 0.0;

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public double getScore() {
        return score;
    }

    @Override
    public void adjustScore(double difference) {
        score += difference;
    }

    @Override
    @Nullable
    public Object get(String key) {
        return data.get(key);
    }

    @Override
    @Nullable
    public String getLocalised(String key, String language, String... altNames) {
        return localized.get(key + "||" + language);
    }

    @Override
    public double[] getCoordinates() {
        return coordinates;
    }

    @Override
    public Object getGeometry()
    {
        try {
            return mapper.readValue(geometry, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double @Nullable [] getExtent() {
        return extent;
    }

    @Override
    public Map<String, Object> getRawData() {
        return Map.of();
    }

    public MockPhotonResult put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public MockPhotonResult putLocalized(String key, String lang, String value) {
        localized.put(key + "||" + lang, value);
        return this;
    }

    public MockPhotonResult putGeometry(String geometry) {
        this.geometry = geometry;
        return this;
    }
}

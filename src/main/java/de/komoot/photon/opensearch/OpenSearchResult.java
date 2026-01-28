package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.PhotonResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@NullMarked
public class OpenSearchResult implements PhotonResult {
    private static final String[] NAME_PRECEDENCE = {"default", "housename", "int", "loc", "reg", "alt", "old"};

    private double score = 0.0;
    private final double @Nullable [] extent;
    private final double[] coordinates;
    @Nullable private final String geometry;
    private final Map<String, Object> infos;
    private final Map<String, Map<String, String>> localeTags;

    OpenSearchResult(double @Nullable [] extent, double[] coordinates, Map<String, Object> infos, Map<String, Map<String, String>> localeTags, @Nullable String geometry) {
        this.extent = extent;
        this.coordinates = coordinates;
        this.infos = infos;
        this.localeTags = localeTags;
        this.geometry = geometry;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    @Nullable
    public Object get(String key) {
        return infos.get(key);
    }

    @Override
    @Nullable
    public String getLocalised(String key, String language) {
        final var map = getMap(key);
        if (map == null) return null;

        if (map.get(language) != null) {
            // language specific field
            return map.get(language);
        }

        if ("name".equals(key)) {
            for (String name : NAME_PRECEDENCE) {
                if (map.containsKey(name))
                    return map.get(name);
            }
        }

        return map.get("default");
    }

    @Override
    @Nullable
    public Map<String, String> getMap(String key) {
        return localeTags.get(key);
    }

    @Override
    public double[] getCoordinates() {
        return coordinates;
    }

    @Nullable
    public String getGeometry() {
        return geometry;
    }

    @Override
    public double @Nullable [] getExtent() {
        return extent;
    }

    @Override
    public Map<String, Object> getRawData() {
        return Map.of(
                "score", score,
                "infos", infos,
                "localeTags", localeTags);
    }
}

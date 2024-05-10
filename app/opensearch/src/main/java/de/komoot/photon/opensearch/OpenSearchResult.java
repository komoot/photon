package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.PhotonResult;

import java.util.Map;

public class OpenSearchResult implements PhotonResult {
    private static final String[] NAME_PRECEDENCE = {"default", "housename", "int", "loc", "reg", "alt", "old"};

    private double score = 0.0;
    private final double[] extent;
    private final double[] coordinates;
    private final Map<String, Object> infos;
    private final Map<String, Map<String, String>> localeTags;

    OpenSearchResult(double extent[], double[] coordinates, Map<String, Object> infos, Map<String, Map<String, String>> localeTags) {
        this.extent = extent;
        this.coordinates = coordinates;
        this.infos = infos;
        this.localeTags = localeTags;
    }

    public OpenSearchResult setScore(double score) {
        this.score = score;
        return this;
    }

    @Override
    public Object get(String key) {
        return infos.get(key);
    }

    @Override
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
    public Map<String, String> getMap(String key) {
        return localeTags.get(key);
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
        return score;
    }
}

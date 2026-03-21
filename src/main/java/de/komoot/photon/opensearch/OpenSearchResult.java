package de.komoot.photon.opensearch;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.komoot.photon.searcher.PhotonResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public class OpenSearchResult implements PhotonResult {
    private static final List<String> KEYS_WITH_LOCALE = List.of(
            DocFields.NAME, DocFields.STREET, DocFields.LOCALITY,
            DocFields.DISTRICT, DocFields.CITY, DocFields.COUNTY,
            DocFields.STATE, DocFields.COUNTRY
    );

    private double opensearchScore = 0.0;
    private double score = 0.0;
    private double @Nullable [] extent = null;
    private double[] coordinates = INVALID_COORDINATES;
    private final Map<String, Object> infos = new HashMap<>();
    private final Map<String, Map<String, String>> localeTags = new HashMap<>();


    record ExtentCoordinates (@JsonProperty("coordinates") double[][] coordinates) {
        public double[] getNW() {
            return coordinates[0];
        }

        public double[] getSE() {
            return coordinates[1];
        }
    }

    record Point (@JsonProperty("lat") double lat, @JsonProperty("lon") double lon) {}

    @Override
    public double getScore() {
        return this.score;
    }

    @Override
    public void adjustScore(double difference) {
        this.score += difference;
    }

    public void setOpensearchScore(double opensearchScore) {
        this.opensearchScore = opensearchScore;
    }

    public double getOpensearchScore() {
        return opensearchScore;
    }

    public double getImportance() {
        Double importance = (Double) infos.get(DocFields.IMPORTANCE);
        return importance == null ? 0.00000001 : importance;
    }

    @Override
    @Nullable
    public Object get(String key) {
        return infos.get(key);
    }

    @Override
    @Nullable
    public String getLocalised(String key, String language, String... altNames) {
        final var map = localeTags.get(key);
        if (map == null) return null;

        String name = map.get(language);
        if (name != null) {
            return name;
        }
        name = map.get("default");
        if (name != null) {
            return name;
        }
        for (var nameKey : altNames) {
            name = map.get(nameKey);
            if (name != null) {
                return name;
            }

        }

        return null;
    }

    @Override
    public double[] getCoordinates() {
        return coordinates;
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


    @JsonProperty(DocFields.COORDINATE)
    void setCoordinates(Point pt) {
        coordinates = new double[]{pt.lon, pt.lat};
    }

    @JsonProperty(DocFields.EXTENT)
    void setExtent(ExtentCoordinates coords) {
        final var nw = coords.getNW();
        final var se = coords.getSE();

        extent = new double[]{nw[0], nw[1], se[0], se[1]};
    }

    @JsonAnySetter
    void setProperties(String key, Object value) {
        if (KEYS_WITH_LOCALE.contains(key)) {
            if (value instanceof Map<?,?>) {
                var names = new HashMap<String, String>();
                for (var entry : ((Map<?, ?>) value).entrySet()) {
                    if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                        names.put((String) entry.getKey(), (String) entry.getValue());
                    }
                }
                localeTags.put(key, names);
            }
        } else {
            infos.put(key, value);
        }
    }
}

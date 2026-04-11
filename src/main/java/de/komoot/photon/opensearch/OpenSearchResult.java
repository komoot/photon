package de.komoot.photon.opensearch;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.komoot.photon.searcher.PhotonResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public class OpenSearchResult implements PhotonResult {
    private static final double EARTH_RADIUS_KM = 2 * 6371;

    private static final List<String> KEYS_WITH_LOCALE = List.of(
            DocFields.NAME, DocFields.STREET, DocFields.LOCALITY,
            DocFields.DISTRICT, DocFields.CITY, DocFields.COUNTY,
            DocFields.STATE, DocFields.COUNTRY
    );

    @Nullable private Double opensearchScore;
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

    record JsonPoint(@JsonProperty("lat") double lat, @JsonProperty("lon") double lon) {}

    @Override
    public double getScore() {
        return this.score;
    }

    @Override
    public void adjustScore(double difference) {
        this.score += difference;
    }

    public void setOpensearchScore(@Nullable Double opensearchScore) {
        this.opensearchScore = opensearchScore;
    }

    public void adjustScoreByImportance(double osScoreWeight) {
        double importance = getImportance();
        if (opensearchScore != null) {
            opensearchScore -= importance * osScoreWeight;
        }
        score += importance;
    }

    public void adjustScoreByLocationBias(Point pt, double osScoreFactor, double weight,
                                          double biasRadius, double negDecayFactor) {
        double bias = 0.0;
        if (coordinates != INVALID_COORDINATES) {
            // compute haversine distance
            double cosY1 = Math.cos(Math.toRadians(pt.getY()));
            double cosY2 = Math.cos(Math.toRadians(coordinates[1]));
            double sinXDist = Math.sin(Math.toRadians(pt.getX() - coordinates[0]) / 2);
            double sinYDist = Math.sin(Math.toRadians(pt.getY() - coordinates[1]) / 2);
            double a = sinYDist * sinYDist + cosY1 * cosY2 * sinXDist * sinXDist;
            double dist = EARTH_RADIUS_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            if (dist < biasRadius) {
                bias = 1.0;
            } else {
                bias = Math.exp((dist - biasRadius) * negDecayFactor);
            }
        }
        bias *= weight;

        if (opensearchScore != null) {
            opensearchScore -= bias * osScoreFactor;
        }

        score += bias;
    }

    public double getOpensearchScore() {
        return opensearchScore == null ? 0.0 : opensearchScore;
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
    void setCoordinates(JsonPoint pt) {
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

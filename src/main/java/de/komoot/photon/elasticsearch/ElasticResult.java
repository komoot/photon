package de.komoot.photon.elasticsearch;

import de.komoot.photon.Constants;
import de.komoot.photon.searcher.PhotonResult;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.SearchHit;

import java.util.List;
import java.util.Map;

@Slf4j
public class ElasticResult implements PhotonResult {
    private static final String[] NAME_PRECEDENCE = {"default", "housename", "int", "loc", "reg", "alt", "old"};

    private final Map<String, Object> source;
    private final double score;

    ElasticResult(Map<String, Object> source, double score) {
        this.score = score;
        this.source = source;
    }

    @Override
    public Object get(String key) {
        return source.get(key);
    }

    @Override
    public String getLocalised(String key, String language) {
        final Map<String, String> map = (Map<String, String>) source.get(key);
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
        return (Map<String, String>) source.get(key);
    }

    @Override
    public double[] getCoordinates() {
        final Map<String, Double> coordinate = (Map<String, Double>) source.get("coordinate");
        if (coordinate == null) {
            log.error(String.format("invalid data [id=%s, type=%s], coordinate is missing!",
                    source.get(Constants.OSM_ID),
                    source.get(Constants.OSM_VALUE)));
            return new double[]{Double.NaN, Double.NaN};
        }

        return new double[]{coordinate.get(Constants.LON), coordinate.get(Constants.LAT)};
    }

    @Override
    public double[] getExtent() {
        final Map<String, Object> extent = (Map<String, Object>) source.get("extent");
        if (extent == null) {
            return null;
        }

        final List<List<Double>> coords = (List<List<Double>>) extent.get("coordinates");
        final List<Double> nw = coords.get(0);
        final List<Double> se = coords.get(1);

        return new double[]{nw.get(0), nw.get(1), se.get(0), se.get(1)};
    }

    @Override
    public double getScore() {
        return score;
    }
}

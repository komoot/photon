package de.komoot.photon.elasticsearch;

import de.komoot.photon.Constants;
import de.komoot.photon.searcher.PhotonResult;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class ElasticResult implements PhotonResult {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ElasticResult.class);

    private static final String[] NAME_PRECEDENCE = {"default", "housename", "int", "loc", "reg", "alt", "old"};

    private final SearchHit result;

    ElasticResult(SearchHit result) {
        this.result = result;
    }

    @Override
    public Object get(String key) {
        return result.getSource().get(key);
    }

    @Override
    public String getLocalised(String key, String language) {
        final Map<String, String> map = (Map<String, String>) result.getSource().get(key);
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
        return (Map<String, String>) result.getSource().get(key);
    }

    @Override
    public double[] getCoordinates() {
        final Map<String, Double> coordinate = (Map<String, Double>) result.getSource().get("coordinate");
        if (coordinate == null) {
            LOGGER.error("invalid data [id={}, type={}], coordinate is missing!",
                    result.getSource().get(Constants.OSM_ID),
                    result.getSource().get(Constants.OSM_VALUE));
            return INVALID_COORDINATES;
        }

        return new double[]{coordinate.get(Constants.LON), coordinate.get(Constants.LAT)};
    }

    @Override
    public double[] getExtent() {
        final Map<String, Object> extent = (Map<String, Object>) result.getSource().get("extent");
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
        return result.getScore();
    }
}

package de.komoot.photon.elasticsearch;

import de.komoot.photon.Constants;
import de.komoot.photon.searcher.PhotonResult;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.search.SearchHit;

import java.util.List;
import java.util.Map;

@Slf4j
public class ElasticResult implements PhotonResult {
    private static final String[] NAME_PRECEDENCE = {"default", "housename", "int", "loc", "reg", "alt", "old"};

    private final Object result;
    private final Map<String, Object> resultMap;

    ElasticResult(SearchHit result) {
        this.result = result;
        this.resultMap = result.getSource();
    }

    ElasticResult(GetResponse result) {
        this.result = result;
        this.resultMap = result.getSource();
    }

    @Override
    public Object get(String key) {
        return resultMap.get(key);
    }

    @Override
    public String getLocalised(String key, String language) {
        final Map<String, String> map = (Map<String, String>) resultMap.get(key);

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
        return (Map<String, String>) resultMap.get(key);
    }

    @Override
    public double[] getCoordinates() {
        final Map<String, Double> coordinate = (Map<String, Double>) resultMap.get("coordinate");
        if (coordinate == null) {
            log.error(String.format("invalid data [id=%s, type=%s], coordinate is missing!",
                    resultMap.get(Constants.OSM_ID),
                    resultMap.get(Constants.OSM_VALUE)));
            return INVALID_COORDINATES;
        }

        return new double[]{coordinate.get(Constants.LON), coordinate.get(Constants.LAT)};
    }

    @Override
    public double[] getExtent() {
        final Map<String, Object> extent = (Map<String, Object>) resultMap.get("extent");
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
        if (result instanceof SearchHit) {
            return ((SearchHit)result).getScore();
        } else {
            return 0.0;
        }
    }
}

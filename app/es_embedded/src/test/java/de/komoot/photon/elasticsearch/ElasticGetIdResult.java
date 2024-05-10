package de.komoot.photon.elasticsearch;

import de.komoot.photon.searcher.PhotonResult;
import org.apache.commons.lang3.NotImplementedException;
import org.elasticsearch.action.get.GetResponse;

import java.util.Map;

public class ElasticGetIdResult implements PhotonResult {

    private final GetResponse result;

    public ElasticGetIdResult(GetResponse result) {
        this.result = result;
    }
    @Override
    public Object get(String key) {
        return result.getSource().get(key);
    }

    @Override
    public String getLocalised(String key, String language) {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, String> getMap(String key) {
        return (Map<String, String>) result.getSource().get(key);
    }

    @Override
    public double[] getCoordinates() {
        throw new NotImplementedException();
    }

    @Override
    public double[] getExtent() {
        throw new NotImplementedException();
    }

    @Override
    public double getScore() {
        throw new NotImplementedException();
    }
}

package de.komoot.photon;

import com.google.common.base.Converter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class ConvertToGeoJson extends Converter<List<JSONObject>,JSONObject> {


    @Override
    protected JSONObject doForward(List<JSONObject> jsonObjects) {
        final JSONObject collection = new JSONObject();
        collection.put("type", "FeatureCollection");
        collection.put("features", new JSONArray(jsonObjects));

        return null;
    }

    @Override
    protected List<JSONObject> doBackward(JSONObject jsonObject) {
        JSONArray features = jsonObject.getJSONArray("features");
        List<JSONObject> returnValue = new ArrayList<JSONObject>(features.length());
        for (int i = 0; i < features.length(); i++) {
            JSONObject aSearchHit = features.getJSONObject(i);
            returnValue.add(aSearchHit);
        }
        return returnValue;
    }
}

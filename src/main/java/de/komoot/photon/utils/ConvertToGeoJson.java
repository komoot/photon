package de.komoot.photon.utils;

import com.google.common.base.Converter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Convert between place list and geojson Created by Sachin Dole on 2/20/2015.
 */
public class ConvertToGeoJson extends Converter<List<JSONObject>, JSONObject> {

    /**
     * converts a list of places to a geojson places. Does not validate the list of places. In other words, if json list does not contain valid places, then resulting geojson *
     * will not be valid.
     *
     * @param listOfPlaces places to be converted to geojson
     *
     * @return geojson object
     */
    @Override
    public JSONObject doForward(List<JSONObject> listOfPlaces) {
        final JSONObject collection = new JSONObject();
        collection.put("type", "FeatureCollection");
        collection.put("features", new JSONArray(listOfPlaces));

        return collection;
    }

    /**
     * Extracts the place information from a geojson object and returns a list of those places. Does not validate that the geojson object is valid or that the contents are * indeed
     * places.
     *
     * @param jsonObject a list of places in geojson format
     *
     * @return the list of places from the geojson.
     */
    @Override
    public List<JSONObject> doBackward(JSONObject jsonObject) {
        JSONArray features = jsonObject.getJSONArray("features");
        List<JSONObject> returnValue = new ArrayList<JSONObject>(features.length());
        for (int i = 0; i < features.length(); i++) {
            JSONObject aSearchHit = features.getJSONObject(i);
            returnValue.add(aSearchHit);
        }
        return returnValue;
    }
}

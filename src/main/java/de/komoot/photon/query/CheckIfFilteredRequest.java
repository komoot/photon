package de.komoot.photon.query;

import spark.QueryParamsMap;

/**
 * Created by Sachin Dole on 2/22/2015.
 */
public class CheckIfFilteredRequest {

    public Boolean execute(QueryParamsMap... operand) {
        QueryParamsMap queryParam = operand[0];
        if (!queryParam.hasValue()) return false;
        String[] tagsToFilterOn = queryParam.values();
        for (String eachTagToFilterOn : tagsToFilterOn) {
            if (eachTagToFilterOn != null && eachTagToFilterOn.trim().length() > 0) {
                return true;
            }
        }
        return false;
    }
}

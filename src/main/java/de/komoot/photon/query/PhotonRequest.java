package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import java.io.Serializable;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonRequest implements Serializable {
    private String query;
    private Integer limit;
    private Point locationForBias;
    private String language;
    private String formula = "";

    public PhotonRequest(String query, Integer limit, Point locationForBias, String formula, String language) {
        this.query = query;
        this.limit = limit;
        this.locationForBias = locationForBias;
        this.formula = formula;
        this.language = language;
    }

    public String getQuery() {
        return query;
    }

    public Integer getLimit() {
        return limit;
    }

    public Point getLocationForBias() {
        return locationForBias;
    }

    public String getFormula() {
        return formula;
    }

    public String getLanguage() {
        return language;
    }
}

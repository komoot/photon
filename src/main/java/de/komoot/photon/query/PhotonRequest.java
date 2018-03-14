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
    private final double scale;

    public PhotonRequest(String query, int limit, Point locationForBias, double scale, String language) {
        this.query = query;
        this.limit = limit;
        this.locationForBias = locationForBias;
        this.scale = scale;
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

    public double getScaleForBias() {
        return scale;
    }

    public String getLanguage() {
        return language;
    }
}

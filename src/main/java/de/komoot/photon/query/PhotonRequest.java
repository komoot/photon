package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import java.io.Serializable;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonRequest implements Serializable {
    private final String query;
    private final int limit;
    private final Point locationForBias;
    private final String language;
    private final double radiusForBias;

    public PhotonRequest(String query, int limit, Point locationForBias, double radiusForBias, String language) {
        this.query = query;
        this.limit = limit;
        this.locationForBias = locationForBias;
        this.radiusForBias = radiusForBias;
        this.language = language;
    }

    public String getQuery() {
        return query;
    }

    public int getLimit() {
        return limit;
    }

    public Point getLocationForBias() {
        return locationForBias;
    }

    public double getRadiusForBias() {
        return radiusForBias;
    }

    public String getLanguage() {
        return language;
    }
}

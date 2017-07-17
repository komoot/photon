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
    private Boolean locationDistanceSort = true;

    public PhotonRequest(String query, Integer limit, Point locationForBias, Boolean locationDistanceSort, String language){
        this.query = query;
        this.limit = limit;
        this.locationForBias = locationForBias;
        this.locationDistanceSort = locationDistanceSort;
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
    
    public Boolean getLocationDistanceSort() {
        return locationDistanceSort;
    }

    public String getLanguage() {
        return language;
    }
}

package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import java.io.Serializable;

/**
 * @author svantulden
 */
public class ReverseRequest implements Serializable {
    private Point location;
    private String language;
    private Double radius;
    private Integer limit;
    private String queryStringFilter;
    private Boolean locationDistanceSort = true;

    public ReverseRequest(Point location, String language, Double radius, String queryStringFilter, Integer limit, Boolean locationDistanceSort) {
        this.location = location;
        this.language = language;
        this.radius = radius;
        this.limit = limit;
        this.queryStringFilter = queryStringFilter;
        this.locationDistanceSort = locationDistanceSort;
    }

    public Point getLocation() {
        return location;
    }

    public String getLanguage() {
        return language;
    }

    public Double getRadius() {
        return radius;
    }

    public Integer getLimit() {
        return limit;
    }

    public String getQueryStringFilter() {
        return queryStringFilter;
    }

    public Boolean getLocationDistanceSort() {
        return locationDistanceSort;
    }
}

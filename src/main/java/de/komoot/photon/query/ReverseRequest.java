package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    private Set<String> includeExtraKeys;

    public ReverseRequest(Point location, String language, Double radius, String queryStringFilter, Integer limit, Boolean locationDistanceSort, String[] extraKeys) {
        this.location = location;
        this.language = language;
        this.radius = radius;
        this.limit = limit;
        this.queryStringFilter = queryStringFilter;
        this.locationDistanceSort = locationDistanceSort;
        this.includeExtraKeys = new HashSet<String>(Arrays.asList(extraKeys));
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

    public Set<String> extraKeys() {
        return includeExtraKeys;
    }
}

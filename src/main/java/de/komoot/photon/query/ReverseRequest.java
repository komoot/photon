package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import java.io.Serializable;

/**
 * @author svantulden
 */
public class ReverseRequest implements Serializable {
    private final Point location;
    private final String language;
    private final double radius;
    private final int limit;
    private final String queryStringFilter;

    public ReverseRequest(Point location, String language, double radius, String queryStringFilter, int limit) {
        this.location = location;
        this.language = language;
        this.radius = radius;
        this.limit = limit;
        this.queryStringFilter = queryStringFilter;
    }

    public Point getLocation() {
        return location;
    }

    public String getLanguage() {
        return language;
    }

    public double getRadius() {
        return radius;
    }

    public int getLimit() {
        return limit;
    }

    public String getQueryStringFilter() {
        return queryStringFilter;
    }
}

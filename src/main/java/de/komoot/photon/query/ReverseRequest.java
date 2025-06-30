package de.komoot.photon.query;

import org.locationtech.jts.geom.Point;

/**
 * Collection of query parameters for a reverse request.
 */
public class ReverseRequest extends RequestBase {
    private Point location;
    private double radius = 1.0;
    private String queryStringFilter;
    private boolean locationDistanceSort = true;

    public ReverseRequest() {
        setLimit(1, 1);
    }

    public Point getLocation() {
        return location;
    }

    public double getRadius() {
        return radius;
    }

    public String getQueryStringFilter() {
        return queryStringFilter;
    }

    public boolean getLocationDistanceSort() {
        return locationDistanceSort;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public void setRadius(Double radius) {
        if (radius != null) {
            this.radius = radius;
        }
    }

    public void setQueryStringFilter(String queryStringFilter) {
        this.queryStringFilter = queryStringFilter;
    }

    public void setLocationDistanceSort(Boolean locationDistanceSort) {
        if (locationDistanceSort != null) {
            this.locationDistanceSort = locationDistanceSort;
        }
    }
}

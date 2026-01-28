package de.komoot.photon.query;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;

/**
 * Collection of query parameters for a reverse request.
 */
@NullMarked
public class ReverseRequest extends RequestBase {
    private final Point location;
    private double radius = 1.0;
    @Nullable private String queryStringFilter;
    private boolean locationDistanceSort = true;

    public ReverseRequest(Point location) {
        this.location = location;
        setLimit(1, 1);
    }

    public Point getLocation() {
        return location;
    }

    public double getRadius() {
        return radius;
    }

    @Nullable
    public String getQueryStringFilter() {
        return queryStringFilter;
    }

    public boolean getLocationDistanceSort() {
        return locationDistanceSort;
    }

    public void setRadius(@Nullable Double radius) {
        if (radius != null) {
            this.radius = radius;
        }
    }

    public void setQueryStringFilter(@Nullable String queryStringFilter) {
        if (queryStringFilter != null) {
            this.queryStringFilter = queryStringFilter;
        }
    }

    public void setLocationDistanceSort(boolean locationDistanceSort) {
        this.locationDistanceSort = locationDistanceSort;
    }
}

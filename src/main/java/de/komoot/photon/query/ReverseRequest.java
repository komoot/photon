package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import de.komoot.photon.searcher.TagFilter;

import java.io.Serializable;
import java.util.*;

/**
 * Collection of query parameters for a reverse request.
 */
public class ReverseRequest implements Serializable {
    private final Point location;
    private final String language;
    private final double radius;
    private final int limit;
    private final String queryStringFilter;
    private final boolean locationDistanceSort;
    private final Set<String> layerFilters;
    private final List<TagFilter> osmTagFilters = new ArrayList<>(1);
    private final boolean debug;

    public ReverseRequest(Point location, String language, double radius, String queryStringFilter, int limit,
                          boolean locationDistanceSort, Set<String> layerFilter, boolean debug) {
        this.location = location;
        this.language = language;
        this.radius = radius;
        this.limit = limit;
        this.queryStringFilter = queryStringFilter;
        this.locationDistanceSort = locationDistanceSort;
        this.layerFilters = layerFilter;
        this.debug = debug;
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

    public boolean getLocationDistanceSort() {
        return locationDistanceSort;
    }

    public Set<String> getLayerFilters() {
        return layerFilters;
    }

    public List<TagFilter> getOsmTagFilters() {
        return osmTagFilters;
    }

    public boolean getDebug() {
        return debug;
    }

    ReverseRequest addOsmTagFilter(TagFilter filter) {
        osmTagFilters.add(filter);
        return this;
    }
}

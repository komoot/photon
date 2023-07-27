package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import de.komoot.photon.searcher.TagFilter;

import java.io.Serializable;
import java.util.*;

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
    private Set<String> layerFilters;
    private final List<TagFilter> osmTagFilters = new ArrayList<>(1);
    private boolean debug;

    public ReverseRequest(Point location, String language, Double radius, String queryStringFilter, Integer limit,
                          Boolean locationDistanceSort, Set<String> layerFilter, boolean debug) {
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

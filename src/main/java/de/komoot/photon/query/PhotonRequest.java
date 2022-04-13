package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.searcher.TagFilter;

import java.util.*;

/**
 * Collection of query parameters for a search request.
 */
public class PhotonRequest {
    private final String query;
    private final String language;
    private int limit = 15;
    private Point locationForBias = null;
    private double scale = 0.2;
    private int zoom = 14;
    private Envelope bbox = null;
    private boolean debug = false;

    private final List<TagFilter> osmTagFilters = new ArrayList<>(1);
    private Set<String> layerFilters = new HashSet<>(1);


    public PhotonRequest(String query, String language) {
        this.query = query;
        this.language = language;
    }

    public String getQuery() {
        return query;
    }

    public int getLimit() {
        return limit;
    }
    
    public Envelope getBbox() {
        return bbox;
    }

    public Point getLocationForBias() {
        return locationForBias;
    }

    public double getScaleForBias() {
        return scale;
    }

    public int getZoomForBias() {
        return zoom;
    }

    public String getLanguage() {
        return language;
    }

    public boolean getDebug() { return debug; }

    public List<TagFilter> getOsmTagFilters() {
        return osmTagFilters;
    }

    public Set<String> getLayerFilters() {
        return layerFilters;
    }

    PhotonRequest addOsmTagFilter(TagFilter filter) {
        osmTagFilters.add(filter);
        return this;
    }

    PhotonRequest setLayerFilter(Set<String> filters) {
        layerFilters = filters;
        return this;
    }

    PhotonRequest setLimit(Integer limit) {
        if (limit != null) {
            this.limit = Integer.max(Integer.min(limit, 50), 1);
        }
        return this;
    }

    PhotonRequest setLocationForBias(Point locationForBias) {
        if (locationForBias != null) {
            this.locationForBias = locationForBias;
        }
        return this;
    }

    PhotonRequest setScale(Double scale) {
        if (scale != null) {
            this.scale = Double.max(Double.min(scale, 1.0), 0.0);
        }
        return this;
    }

    PhotonRequest setZoom(Integer zoom) {
        if (zoom != null) {
            this.zoom = Integer.max(Integer.min(zoom, 18), 0);
        }
        return this;
    }

    PhotonRequest setBbox(Envelope bbox) {
        if (bbox != null) {
            this.bbox = bbox;
        }
        return this;
    }

    PhotonRequest enableDebug() {
        this.debug = true;
        return this;
    }
}

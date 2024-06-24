package de.komoot.photon.query;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import de.komoot.photon.searcher.TagFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhotonRequestBase
{
    private final String language;
    private int limit = 15;
    private Point locationForBias = null;
    private double scale = 0.2;
    private int zoom = 14;
    private Envelope bbox = null;
    private boolean debug = false;

    private final List<TagFilter> osmTagFilters = new ArrayList<>(1);
    private Set<String> layerFilters = new HashSet<>(1);

    protected PhotonRequestBase(String language)
    {
        this.language = language;
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

    void addOsmTagFilter(TagFilter filter) {
        osmTagFilters.add(filter);
    }

    void setLayerFilter(Set<String> filters) {
        layerFilters = filters;
    }

    void setLimit(Integer limit) {
        this.limit = limit;
    }

    void setLocationForBias(Point locationForBias) {
        if (locationForBias != null) {
            this.locationForBias = locationForBias;
        }
    }

    void setScale(Double scale) {
        if (scale != null) {
            this.scale = Double.max(Double.min(scale, 1.0), 0.0);
        }
    }

    void setZoom(Integer zoom) {
        if (zoom != null) {
            this.zoom = Integer.max(Integer.min(zoom, 18), 0);
        }
    }

    void setBbox(Envelope bbox) {
        if (bbox != null) {
            this.bbox = bbox;
        }
    }

    void enableDebug() {
        this.debug = true;
    }
}

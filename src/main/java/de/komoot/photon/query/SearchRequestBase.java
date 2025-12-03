package de.komoot.photon.query;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;

public class SearchRequestBase extends RequestBase {
    private Point locationForBias = null;
    private double scale = 0.2;
    private int zoom = 14;
    private Envelope bbox = null;
    private boolean includeHousenumbers = false;

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

    public boolean getIncludeHousenumbers() {
        return includeHousenumbers;
    }

    public void setIncludeHousenumbers(boolean includeHousenumbers) {
        this.includeHousenumbers = includeHousenumbers;
    }
}

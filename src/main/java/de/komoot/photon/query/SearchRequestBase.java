package de.komoot.photon.query;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;

@NullMarked
public class SearchRequestBase extends RequestBase {
    @Nullable private Point locationForBias;
    private double scale = 0.2;
    private int zoom = 14;
    @Nullable private Envelope bbox;
    private boolean suggestAddresses = false;

    @Nullable
    public Envelope getBbox() {
        return bbox;
    }

    @Nullable
    public Point getLocationForBias() {
        return locationForBias;
    }

    public double getScaleForBias() {
        return scale;
    }

    public int getZoomForBias() {
        return zoom;
    }

    void setLocationForBias(@Nullable Point locationForBias) {
        if (locationForBias != null) {
            this.locationForBias = locationForBias;
        }
    }

    void setScale(@Nullable Double scale) {
        if (scale != null) {
            this.scale = Double.max(Double.min(scale, 1.0), 0.0);
        }
    }

    void setZoom(@Nullable Integer zoom) {
        if (zoom != null) {
            this.zoom = Integer.max(Integer.min(zoom, 18), 0);
        }
    }

    void setBbox(@Nullable Envelope bbox) {
        if (bbox != null) {
            this.bbox = bbox;
        }
    }

    public boolean getSuggestAddresses() {
        return suggestAddresses;
    }

    public void setSuggestAddresses(boolean suggestAddresses) {
        this.suggestAddresses = suggestAddresses;
    }
}

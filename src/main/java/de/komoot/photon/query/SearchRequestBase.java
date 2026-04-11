package de.komoot.photon.query;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;

@NullMarked
public class SearchRequestBase extends RequestBase {
    @Nullable private Point locationForBias;
    private float scale = 0.2f;
    private int zoom = 12;
    private double biasRadius = zoomToRadius(zoom);
    @Nullable private Envelope bbox;
    private boolean suggestAddresses = false;

    private static double zoomToRadius(int zoom) {
        return Math.pow(2.2, 18 - zoom) * 0.1;
    }

    @Nullable
    public Envelope getBbox() {
        return bbox;
    }

    public boolean hasLocationBias() {
        return locationForBias != null && zoom > 4;
    }

    @Nullable
    public Point getLocationForBias() {
        return locationForBias;
    }

    public double getRadiusForBias() {
        return biasRadius;
    }

    public double getDecayRadiusForBias() {
        return biasRadius * (zoom - 4);
    }

    public float getImportanceWeight() {
        return hasLocationBias() ? scale : 1.0f;
    }

    void setLocationForBias(@Nullable Point locationForBias) {
        if (locationForBias != null) {
            this.locationForBias = locationForBias;
        }
    }

    void setScale(@Nullable Double scale) {
        if (scale != null) {
            this.scale = (float) Double.max(Double.min(scale, 1.0), 0.0);
        }
    }

    void setZoom(@Nullable Integer zoom) {
        if (zoom != null) {
            this.zoom = Integer.max(Integer.min(zoom, 18), 0);
            biasRadius = zoomToRadius(this.zoom);
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

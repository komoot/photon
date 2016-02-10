package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import java.io.Serializable;

/**
 *
 * @author svantulden
 */
public class ReverseRequest implements Serializable {
    private Point location;
    private String language;
    private Double radius;
    private Integer limit;

    public ReverseRequest(Point location, String language, Double radius, Integer limit){
        this.location = location;
        this.language = language;
        this.radius = radius;
        this.limit = limit;
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
}

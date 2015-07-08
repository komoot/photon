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

    public ReverseRequest(Point location, String language){
        this.location = location;
        this.language = language;
    }

    public Point getLocation() {
        return location;
    }

    public String getLanguage() {
        return language;
    }
}

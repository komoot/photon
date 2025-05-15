package de.komoot.photon.query;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import spark.Request;

/**
 * Convertor which transforms lon/lat parameter into a location and validates the given coordinates.
 */
public class LocationParamConverter {
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private boolean mandatory;

    public LocationParamConverter(boolean mandatory) {
        this.mandatory = mandatory;
    }
    
    public Point apply(Request webRequest) throws BadRequestException {
        Point location;
        String lonParam = webRequest.queryParams("lon");
        String latParam = webRequest.queryParams("lat");
        if (!mandatory && lonParam == null && latParam == null) {
            return null;
        }
        
        try {
            Double lon = Double.valueOf(lonParam);
            if (Double.isNaN(lon) || lon > 180.0 || lon < -180.00) {
                throw new BadRequestException(400, "invalid search term 'lon', expected number >= -180.0 and <= 180.0");
            }
            Double lat = Double.valueOf(latParam);
            if (Double.isNaN(lat) || lat > 90.0 || lat < -90.00) {
                throw new BadRequestException(400, "invalid search term 'lat', expected number >= -90.0 and <= 90.0");
            }
            location = geometryFactory.createPoint(new Coordinate(lon, lat));
        } catch (NullPointerException | NumberFormatException e) {
            throw new BadRequestException(400, "invalid search term 'lat' and/or 'lon', try instead lat=51.5&lon=8.0");
        }
        return location;
    }
}

package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;

import spark.Request;

/**
 * Converts the bbox parameter into an Envelope and performs format checking.
 * Created by Holger Bruch on 10/13/2018.
 */
public class BoundingBoxParamConverter {

    public static final String INVALID_BBOX_ERROR_MESSAGE = "invalid number of supplied coordinates for parameter 'bbox', expected format is: minLon,minLat,maxLon,maxLat";
    public static final String INVALID_BBOX_BOUNDS_MESSAGE = "Invalid bounds for parameter bbox, expected values minLat, maxLat element [-90,90], minLon, maxLon element [-180,180]";

    public Envelope apply(Request webRequest) throws BadRequestException {
        String bboxParam = webRequest.queryParams("bbox");
        if (bboxParam == null) {
            return null;
        }
        Envelope bbox = null;
        try {
            String[] bboxCoords = bboxParam.split(",");
            if (bboxCoords.length != 4) {
                throw new BadRequestException(400, INVALID_BBOX_ERROR_MESSAGE);
            }
            Double minLon = Double.valueOf(bboxCoords[0]);
            Double minLat = Double.valueOf(bboxCoords[1]);
            Double maxLon = Double.valueOf(bboxCoords[2]);
            Double maxLat = Double.valueOf(bboxCoords[3]);
            if (minLon > 180.0 || minLon < -180.00 || maxLon > 180.0 || maxLon < -180.00) {
                throw new BadRequestException(400, INVALID_BBOX_BOUNDS_MESSAGE);
            }
            if (minLat > 90.0 || minLat < -90.00 || maxLat > 90.0 || maxLat < -90.00) {
                throw new BadRequestException(400, INVALID_BBOX_BOUNDS_MESSAGE);
            }
            bbox = new Envelope(minLon, maxLon, minLat, maxLat);
        } catch (NumberFormatException nfe) {
            throw new BadRequestException(400, INVALID_BBOX_ERROR_MESSAGE);
        }

        return bbox;
    }
}

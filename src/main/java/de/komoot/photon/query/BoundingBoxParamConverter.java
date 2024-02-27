package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;

import spark.Request;

/**
 * Converter which transforms a bbox parameter into an Envelope and performs format checking.
 */
public class BoundingBoxParamConverter {

    public static final String INVALID_BBOX_ERROR_MESSAGE = "Invalid number of supplied coordinates for parameter 'bbox', expected format is: minLon,minLat,maxLon,maxLat";
    public static final String INVALID_BBOX_BOUNDS_MESSAGE = "Invalid bounds for parameter 'bbox', expected values minLat, maxLat element [-90,90], minLon, maxLon element [-180,180]";

    public Envelope apply(Request webRequest) throws BadRequestException {
        String bboxParam = webRequest.queryParams("bbox");
        if (bboxParam == null) {
            return null;
        }

        String[] bboxCoords = bboxParam.split(",");
        if (bboxCoords.length != 4) {
            throw new BadRequestException(400, INVALID_BBOX_ERROR_MESSAGE);
        }

        return new Envelope(parseDouble(bboxCoords[0], 180),
                parseDouble(bboxCoords[2], 180),
                parseDouble(bboxCoords[1], 90),
                parseDouble(bboxCoords[3], 90));
    }

    private double parseDouble(String coord, double limit) throws BadRequestException {
        double result;
        try {
            result = Double.parseDouble(coord);
        } catch (NumberFormatException nfe) {
            throw new BadRequestException(400, INVALID_BBOX_ERROR_MESSAGE);
        }

        if (Double.isNaN(result) || result < -limit || result > limit) {
            throw new BadRequestException(400, INVALID_BBOX_BOUNDS_MESSAGE);
        }

        return result;
    }
}

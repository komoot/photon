package de.komoot.photon.query;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Envelope;

/**
 * Converter which transforms a bbox parameter into an Envelope and performs format checking.
 */
@NullMarked
public class BoundingBoxParamConverter {

    public static final String INVALID_BBOX_ERROR_MESSAGE = "Invalid number of supplied coordinates for parameter 'bbox', expected format is: minLon,minLat,maxLon,maxLat";
    public static final String INVALID_BBOX_BOUNDS_MESSAGE = "Invalid bounds for parameter 'bbox', expected values minLat, maxLat element [-90,90], minLon, maxLon element [-180,180]";

    @Nullable
    public static Envelope apply(@Nullable String bboxParam) {
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

    private static double parseDouble(String coord, double limit) {
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

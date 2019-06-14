package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;

import de.komoot.photon.utils.Function;
import spark.Request;

/**
 * Converts the bbox parameter into an Envelope and performs format checking.
 * Created by Holger Bruch on 10/13/2018.
 */
public class BoundingBoxParamConverter implements Function<Request, Envelope, BadRequestException> {

    private static final String INVALID_BBOX_ERROR_MESSAGE = "invalid search term 'bbox', expected format is: minLon,minLat,maxLon,maxLat";

    @Override
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
                throw new BadRequestException(400, INVALID_BBOX_ERROR_MESSAGE);
            }
            if (minLat > 90.0 || minLat < -90.00 || maxLat > 90.0 || maxLat < -90.00) {
                throw new BadRequestException(400, INVALID_BBOX_ERROR_MESSAGE);
            }
            bbox = new Envelope(minLon, maxLon, minLat, maxLat);
        } catch (NumberFormatException nfe) {
            throw new BadRequestException(400, INVALID_BBOX_ERROR_MESSAGE);
        }

        return bbox;
    }
}

package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import java.util.Set;
import spark.Request;

/**
 *
 * @author svantulden
 */
public class ReverseRequestFactory {
    private final LanguageChecker languageChecker;
    private final static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public ReverseRequestFactory(Set<String> supportedLanguages) {
        this.languageChecker = new LanguageChecker(supportedLanguages);
    }
       
    public <R extends ReverseRequest> R create(Request webRequest) throws BadRequestException {
        String language = webRequest.queryParams("lang");
        language = language == null ? "en" : language;
        languageChecker.apply(language);
        
        Point location = null;
        try {
            Double lon = Double.valueOf(webRequest.queryParams("lon"));
            Double lat = Double.valueOf(webRequest.queryParams("lat"));
            location = geometryFactory.createPoint(new Coordinate(lon, lat));
        } catch (Exception nfe) {
            throw new BadRequestException(400, "missing search term 'lat' and/or 'lon': /?lat=51.5&lon=8.0");
        }

        Double radius = 1d;
        String radiusParam = webRequest.queryParams("radius");
        if(radiusParam != null){
            try {
                radius = Double.valueOf(radiusParam);
            } catch (Exception nfe) {
                throw new BadRequestException(400, "invalid search parameter 'radius', expected a number.");
            }
            if(radius < 0){
                throw new BadRequestException(400, "invalid search parameter 'radius', expected a positive number.");
            }else{
                // limit search radius to 5km
                radius = Math.min(radius, 5d);
            }
        }

        Integer limit = 1;
        String limitParam = webRequest.queryParams("limit");
        if(limitParam != null){
            try {
                limit = Integer.valueOf(limitParam);
            } catch (Exception nfe) {
                throw new BadRequestException(400, "invalid search parameter 'limit', expected an integer.");
            }
            if(limit < 0){
                throw new BadRequestException(400, "invalid search parameter 'limit', expected a positive integer.");
            }else{
                // limit number of results to 50
                limit = Math.min(limit, 50);
            }
        }
        ReverseRequest reverseRequest = new ReverseRequest(location, language, radius, limit);
        
        return (R) reverseRequest;
    }
}

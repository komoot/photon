package de.komoot.photon;

import spark.Request;

import java.io.Serializable;

/**
 * Created by sachi_000 on 1/26/2015.
 */
public class PhotonRequest implements Serializable {
    private LocationBias locationBias;
    private String query;
    private Integer limit;

    public PhotonRequest(Request webRequest) throws BadRequestException {
        this.query = webRequest.queryParams("q");
        if (query == null) throw new BadRequestException("missing search term 'q': /?q=berlin");
        try {
            this.limit = Integer.valueOf(webRequest.queryParams("limit"));
        } catch (NumberFormatException e) {
            this.limit = 15;
        }
        Double lon = null;
        Double lat = null;
        try {
            lon = Double.valueOf(webRequest.queryParams("lon"));
            lat = Double.valueOf(webRequest.queryParams("lat"));
            locationBias = new LocationBias(lon, lat);
        } catch (Exception nfe) {
            //ignore
        }
    }

    public LocationBias getLocationBias() {
        return locationBias;
    }

    public String getQuery() {
        return query;
    }

    public Integer getLimit() {
        return limit;
    }

    public class LocationBias {
        private final double lat;
        private final double lon;

        LocationBias(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public double getLat() {
            return lat;
        }
    }
}

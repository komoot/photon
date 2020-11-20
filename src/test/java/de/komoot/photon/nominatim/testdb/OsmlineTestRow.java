package de.komoot.photon.nominatim.testdb;

import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;

@Getter
public class OsmlineTestRow {
    private static long place_id_sequence = 100000;

    private Long placeId;
    private Long parentPlaceId;
    private Long osmId = 23L;
    private Integer startnumber;
    private Integer endnumber;
    private String interpolationtype;
    private String countryCode = "de";
    private String lineGeo;

    public OsmlineTestRow() {
        placeId = place_id_sequence++;
        lineGeo = "LINESTRING(0 0, 0.1 0.1, 0 0.2)";
    }

    public OsmlineTestRow number(int start, int end, String type) {
        startnumber = start;
        endnumber = end;
        interpolationtype = type;
        return this;
    }

    public OsmlineTestRow parent(PlacexTestRow row) {
        parentPlaceId = row.getPlaceId();
        return this;
    }

    public OsmlineTestRow geom(String geom) {
        lineGeo = geom;
        return this;
    }

    public OsmlineTestRow add(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO location_property_osmline (place_id, parent_place_id, osm_id,"
                        + " startnumber, endnumber, interpolationtype, linegeo, country_code)"
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                placeId, parentPlaceId, osmId, startnumber, endnumber, interpolationtype, lineGeo, countryCode);

        return this;
    }
}

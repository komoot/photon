package de.komoot.photon.nominatim.testdb;

import org.jspecify.annotations.NullMarked;
import org.springframework.jdbc.core.JdbcTemplate;

@NullMarked
public class PostcodeLocationTestRow {
    private static long placeIdSequence = 50000;
    private final long placeId;
    private Long parentPlaceId;
    private Long osmId;
    private int rank_search = 16;
    private int indexed_status = 0;
    private final String country_code;
    private final String postcode;
    private String centroid = "POINT (1.0 34.0)";
    private String geometry = "POLYGON ((0.999 33.999, 0.999 34.001, 1.001 34.001, 1.001 33.999, 0.999 33.999))";

    public PostcodeLocationTestRow(String postcode, String country_code) {
        placeId = placeIdSequence++;
        this.postcode = postcode;
        this.country_code = country_code;
    }

    public PostcodeLocationTestRow add(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO location_postcodes
                    (place_id, parent_place_id, osm_id, rank_search,
                    indexed_status, country_code, postcode, centroid, geometry)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                placeId, parentPlaceId, osmId, rank_search, indexed_status,
                country_code, postcode, centroid, geometry
                );

        return this;
    }

    public PostcodeLocationTestRow parent(long parentPlaceId) {
        this.parentPlaceId = parentPlaceId;
        return this;
    }

    public PostcodeLocationTestRow relation(long id) {
        this.osmId = id;
        return this;
    }

    public PostcodeLocationTestRow centroid(double x, double y) {
        centroid = "POINT(" + x + " " + y + ")";
        return this;
    }

    public PostcodeLocationTestRow geometry(String geometry) {
        this.geometry = geometry;
        return this;
    }


    public String getPlaceId() {
        return Long.toString(placeId);
    }
}

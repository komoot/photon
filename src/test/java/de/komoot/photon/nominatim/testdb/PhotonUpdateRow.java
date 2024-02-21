package de.komoot.photon.nominatim.testdb;

import org.springframework.jdbc.core.JdbcTemplate;

public class PhotonUpdateRow {
    private String rel;
    private Long placeId;
    private String operation;


    public PhotonUpdateRow(String rel, Long placeId, String operation) {
        this.rel = rel;
        this.placeId = placeId;
        this.operation = operation;
    }

     public PhotonUpdateRow add(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO photon_updates (rel, place_id, operation, indexed_date)"
                        + "VALUES (?, ?, ?, now())",
                    rel, placeId, operation);

        return this;
    }
}

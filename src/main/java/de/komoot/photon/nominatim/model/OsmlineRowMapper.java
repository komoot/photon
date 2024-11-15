package de.komoot.photon.nominatim.model;

import de.komoot.photon.PhotonDoc;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OsmlineRowMapper implements RowMapper<PhotonDoc> {
    @Override
    public PhotonDoc mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PhotonDoc(
                rs.getLong("place_id"),
                "W", rs.getLong("osm_id"),
                "place", "house_number")
                .parentPlaceId(rs.getLong("parent_place_id"))
                .countryCode(rs.getString("country_code"))
                .postcode(rs.getString("postcode"));
    }
}
package de.komoot.photon.nominatim.model;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.DBDataAdapter;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps the basic attributes of a placex table row to a PhotonDoc.
 *
 * This class does not complete address information (neither country information)
 * for the place.
 */
public class PlaceRowMapper implements RowMapper<PhotonDoc> {

    private final DBDataAdapter dbutils;
    private boolean useGeometryColumn;

    public PlaceRowMapper(DBDataAdapter dbutils) {
        this.dbutils = dbutils;
    }

    public PlaceRowMapper(DBDataAdapter dbutils, boolean useGeometryColumn) {
        this.dbutils = dbutils;
        this.useGeometryColumn = useGeometryColumn;
    }

    @Override
    public PhotonDoc mapRow(ResultSet rs, int rowNum) throws SQLException {
        PhotonDoc doc = new PhotonDoc(rs.getLong("place_id"),
                rs.getString("osm_type"), rs.getLong("osm_id"),
                rs.getString("class"), rs.getString("type"))
                .names(dbutils.getMap(rs, "name"))
                .extraTags(dbutils.getMap(rs, "extratags"))
                .bbox(dbutils.extractGeometry(rs, "bbox"))
                .parentPlaceId(rs.getLong("parent_place_id"))
                .countryCode(rs.getString("country_code"))
                .centroid(dbutils.extractGeometry(rs, "centroid"))
                .rankAddress(rs.getInt("rank_address"))
                .postcode(rs.getString("postcode"));

        if (useGeometryColumn) {
            try {
                doc.geometry(dbutils.extractGeometry(rs, "geometry"));
            } catch (IllegalArgumentException e) {
                System.out.println("Could not get Geometry: " + e);
            }
        }

        double importance = rs.getDouble("importance");
        doc.importance(rs.wasNull() ? (0.75 - rs.getInt("rank_search") / 40d) : importance);

        return doc;
    }
}

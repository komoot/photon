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
    private final String[] languages;
    private final boolean useGeometryColumn;

    public PlaceRowMapper(DBDataAdapter dbutils, String[] langauges, boolean useGeometryColumn) {
        this.dbutils = dbutils;
        this.languages = langauges;
        this.useGeometryColumn = useGeometryColumn;
    }

    @Override
    public PhotonDoc mapRow(ResultSet rs, int rowNum) throws SQLException {
        PhotonDoc doc = new PhotonDoc(rs.getLong("place_id"),
                rs.getString("osm_type"), rs.getLong("osm_id"),
                rs.getString("class"), rs.getString("type"))
                .names(NameMap.makeForPlace(dbutils.getMap(rs, "name"), languages))
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

    public String makeBaseSelect() {
        var sql = "SELECT p.place_id, p.osm_type, p.osm_id, p.class, p.type, p.name, p.postcode, p.admin_level," +
                "       p.address, p.extratags, ST_Envelope(p.geometry) AS bbox, p.parent_place_id," +
                "       p.rank_address, p.rank_search, p.importance, p.country_code, p.centroid, " +
                dbutils.jsonArrayFromSelect(
                        "address_place_id",
                        "FROM place_addressline pa " +
                                " WHERE pa.place_id IN (p.place_id, " +
                                "coalesce(CASE WHEN p.rank_search = 30 THEN p.parent_place_id ELSE null END, p.place_id)) AND isaddress" +
                                " ORDER BY cached_rank_address DESC") + " as addresslines";

        if (useGeometryColumn) {
            sql += ", p.geometry";
        }

        return sql;
    }
}

package de.komoot.photon.nominatim.model;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.DBDataAdapter;
import org.jspecify.annotations.NullMarked;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Access to postcode tables prior to Nominatim 5.3.
 * <p/>
 * Table only contains guessed postcodes.
 */
@NullMarked
public class PostcodeOldStyleRowMapper implements NominatimTableAccessor {
    private final DBDataAdapter dbutils;

    public PostcodeOldStyleRowMapper(DBDataAdapter dbutils) {
        this.dbutils = dbutils;
    }

    @Override
    public PhotonDoc rowToDoc(ResultSet rs) throws SQLException {
        var centroid = dbutils.extractGeometry(rs, "geometry");
        assert centroid != null;
        return new PhotonDoc(
                Long.toString(rs.getLong("place_id")),
                null, -1,
                "place", "postcode")
                .names(NameMap.makeForPlace(Map.of("name", rs.getString("postcode")), List.of()))
                .centroid(centroid)
                .countryCode(rs.getString("country_code"))
                .categories(List.of("osm.place.postcode"))
                .bbox(centroid);
    }

    public String makeBaseQuery(String countrySQLWhere) {
        return """
                SELECT p.place_id, p.parent_place_id, p.postcode, p.rank_search,
                       p.country_code, p.geometry,
                       parent.class as parent_class, parent.type as parent_type,
                       parent.rank_address as parent_rank_address, parent.name as parent_name,
                """
                + dbutils.jsonArrayFromSelect(
                "address_place_id",
                "FROM place_addressline pa " +
                        " WHERE pa.place_id = p.parent_place_id AND isaddress" +
                        " ORDER BY cached_rank_address DESC")
                + """
                  as addresslines FROM location_postcode p
                                     LEFT JOIN placex parent ON p.parent_place_id = parent.place_id
                  """
                + " WHERE p." + countrySQLWhere;
    }

}

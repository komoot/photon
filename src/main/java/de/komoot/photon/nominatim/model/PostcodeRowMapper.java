package de.komoot.photon.nominatim.model;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.DBDataAdapter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Access to postcode tables for Nominatim 5.3+.
 * <p/>
 * This table is now the only source for postcodes nad may contain full
 * geometries for postcode areas.
 */
@NullMarked
public class PostcodeRowMapper implements NominatimTableAccessor {
    private final DBDataAdapter dbutils;
    private final boolean useGeometryColumn;

    public PostcodeRowMapper(DBDataAdapter dbutils, boolean useGeometryColumn) {
        this.dbutils = dbutils;
        this.useGeometryColumn = useGeometryColumn;
    }

    @Override
    public PhotonDoc rowToDoc(ResultSet rs) throws SQLException {
        var osmId = rs.getObject("osm_id");
        var place = new PhotonDoc(
                Long.toString(rs.getLong("place_id")),
                osmId == null ? null : "R",
                osmId == null ? -1 : (Long) osmId,
                "place", "postcode")
                .names(NameMap.makeForPlace(Map.of("name", rs.getString("postcode")), List.of()))
                .centroid(Objects.requireNonNull(dbutils.extractGeometry(rs, "centroid")))
                .countryCode(rs.getString("country_code"))
                .categories(List.of("osm.place.postcode"))
                .importance(0.40001 - rs.getInt("rank_search") / 75d);

        if (useGeometryColumn && osmId != null) {
            // exact geometries are only available for postcode relations
            place.geometry(dbutils.extractGeometry(rs, "geometry"));
        } else {
            place.bbox(dbutils.extractGeometry(rs, "geometry"));
        }

        return place;
    }

    @Override
    public String makeBaseQuery(String sqlWhere) {
        return """
                SELECT p.place_id, p.parent_place_id, p.osm_id, p.postcode,
                       p.country_code, p.centroid, p.geometry, p.rank_search,
                       parent.class as parent_class, parent.type as parent_type,
                       parent.rank_address as parent_rank_address, parent.name as parent_name,
                """
                + dbutils.jsonArrayFromSelect(
                        "address_place_id",
                        "FROM place_addressline pa " +
                                " WHERE pa.place_id = p.parent_place_id AND isaddress" +
                                " ORDER BY cached_rank_address DESC")
                + """
                  as addresslines FROM location_postcodes p
                   LEFT JOIN placex parent ON p.parent_place_id = parent.place_id
                  """
                + " WHERE " + sqlWhere;
    }
}

package de.komoot.photon.nominatim.model;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.DBDataAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OsmlineRowMapper implements RowMapper<PhotonDoc> {
    @Override
    @NotNull
    public PhotonDoc mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PhotonDoc(
                rs.getLong("place_id"),
                "W", rs.getLong("osm_id"),
                "place", "house_number")
                .countryCode(rs.getString("country_code"))
                .categories(List.of("osm.place.house_number"))
                .postcode(rs.getString("postcode"));
    }

    public String makeBaseQuery(DBDataAdapter dbutils) {
        return "SELECT p.place_id, p.osm_id, p.startnumber, p.endnumber," +
                "      p.postcode, p.country_code, p.address, p.linegeo, p.step," +
                "      parent.class as parent_class, parent.type as parent_type," +
                "      parent.rank_address as parent_rank_address, parent.name as parent_name, " +
                dbutils.jsonArrayFromSelect(
                        "address_place_id",
                        "FROM place_addressline pa " +
                                " WHERE pa.place_id IN (p.place_id, coalesce(p.parent_place_id, p.place_id)) AND isaddress" +
                                " ORDER BY cached_rank_address DESC, pa.place_id = p.place_id DESC") + " as addresslines" +
                " FROM location_property_osmline p LEFT JOIN placex parent ON p.parent_place_id = parent.place_id" +
                " WHERE startnumber is not null";
    }
}
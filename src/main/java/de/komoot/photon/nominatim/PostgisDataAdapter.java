package de.komoot.photon.nominatim;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import org.postgis.jts.JtsGeometry;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * utility functions to parse data from postgis
 *
 * @author christoph
 */
public class PostgisDataAdapter implements DBDataAdapter {

    @Override
    public Map<String, String> getMap(ResultSet rs, String columnName) throws SQLException {
        Map<String, String> map = (Map<String, String>) rs.getObject(columnName);
        if (map == null) {
            return Maps.newHashMap();
        }

        return map;
    }

    @Nullable
    @Override
    public Geometry extractGeometry(ResultSet rs, String columnName) throws SQLException {
        JtsGeometry geom = (JtsGeometry) rs.getObject(columnName);
        if (geom == null) {
            return null;
        }
        return geom.getGeometry();
    }

    @Override
    public String addressSQL(boolean hasAddressTags, boolean hasCentroid) {
        String sql = "SELECT DISTINCT ON (rank_address) * FROM ("
                     + "SELECT p.name, p.class, p.type, p.rank_address"
                     + " FROM placex p, place_addressline pa"
                     + " WHERE p.place_id = pa.address_place_id"
                     + "    AND pa.place_id IN (?, ?)"
                     + "    AND pa.address_place_id != ?"
                     + "    AND p.linked_place_id is null"
                     + " ORDER BY p.rank_address desc,"
                     + "          (CASE ";
        if (hasAddressTags) {
            sql += "                    WHEN coalesce(avals(p.name) && ?::text[], False) THEN 2";
        }
        sql +=   "                      WHEN pa.isaddress THEN 0";
        if (hasCentroid) {
            sql += "                    WHEN pa.fromarea and ST_Contains(p.geometry, ST_SetSrid(?::geometry, 4326)) THEN 1";
        }
        sql +=   "                ELSE -1 END) desc,"
               + "          pa.fromarea desc, pa.distance asc, p.rank_search desc) adr";

        return sql;
    }
}

package de.komoot.photon.nominatim.testdb;

import org.locationtech.jts.geom.Geometry;
import de.komoot.photon.nominatim.DBDataAdapter;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class H2DataAdapter implements DBDataAdapter {

    @Override
    public Map<String, String> getMap(ResultSet rs, String columnName) throws SQLException {
        Map<String, String> out = new HashMap<>();
        String json = rs.getString(columnName);
        if (json != null) {
            JSONObject obj = new JSONObject(json);
            for (String key : obj.keySet()) {
                out.put(key, obj.getString(key));
            }
        }

        return out;
    }

    @Override
    public Geometry extractGeometry(ResultSet rs, String columnName) throws SQLException {
        return (Geometry) rs.getObject(columnName);
    }

    @Override
    public boolean hasColumn(JdbcTemplate template, String table, String column)
    {
        if ("location_property_osmline".equals(table) && "step".equals(column)) {
            return true;
        }
        return false;
    }

    @Override
    public String deleteReturning(String deleteSQL, String columns) {
        return "SELECT " + columns + " FROM OLD TABLE (" + deleteSQL + ")";
    }

}

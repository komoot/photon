package de.komoot.photon.nominatim.testdb;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
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
        Object data = rs.getObject(columnName);

        if (data instanceof Geometry) {
            return (Geometry) rs.getObject(columnName);
        }
        return null;
    }

    @Override
    public boolean hasColumn(JdbcTemplate template, String table, String column) {
        return false;
    }
}

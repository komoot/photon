package de.komoot.photon.nominatim.testdb;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import de.komoot.photon.nominatim.DBDataAdapter;
import org.json.JSONObject;

import javax.annotation.Nullable;
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

    @Nullable
    @Override
    public Geometry extractGeometry(ResultSet rs, String columnName) throws SQLException {
        String wkt = (String) rs.getObject(columnName);
        if (wkt != null) {
            try {
                return new WKTReader().read(wkt);
            } catch (ParseException e) {
                // ignore
            }
        }

        return null;
    }
}

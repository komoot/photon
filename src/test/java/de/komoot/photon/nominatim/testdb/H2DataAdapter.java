package de.komoot.photon.nominatim.testdb;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Geometry;
import de.komoot.photon.nominatim.DBDataAdapter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@NullMarked
public class H2DataAdapter implements DBDataAdapter {
    private static final TypeReference<HashMap<String, String>> mapTypeRef = new TypeReference<>() {};

    @Override
    public Map<String, String> getMap(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return json == null ? Map.of() : JsonMapper.shared().readValue(rs.getString(columnName), mapTypeRef);
    }

    @Nullable
    @Override
    public Geometry extractGeometry(ResultSet rs, String columnName) throws SQLException {
        return (Geometry) rs.getObject(columnName);
    }

    @Override
    public String deleteReturning(String deleteSQL, String columns) {
        return "SELECT " + columns + " FROM OLD TABLE (" + deleteSQL + ")";
    }

    @Override
    public String jsonArrayFromSelect(String valueSQL, String fromSQL) {
        return "json_array((SELECT " + valueSQL + " " + fromSQL + ") FORMAT JSON)";
    }
}

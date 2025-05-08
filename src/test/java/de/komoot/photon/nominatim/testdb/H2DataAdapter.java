package de.komoot.photon.nominatim.testdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Geometry;
import de.komoot.photon.nominatim.DBDataAdapter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class H2DataAdapter implements DBDataAdapter {
    private static final TypeReference<HashMap<String, String>> mapTypeRef = new TypeReference<>() {};
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public Map<String, String> getMap(ResultSet rs, String columnName) throws SQLException {
        Map<String, String> out = new HashMap<>();
        String json = rs.getString(columnName);
        try {
            return json == null ? Map.of() : objectMapper.readValue(rs.getString(columnName), mapTypeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public String jsonArrayFromSelect(String valueSQL, String fromSQL) {
        return "json_array((SELECT " + valueSQL + " " + fromSQL + ") FORMAT JSON)";
    }
}

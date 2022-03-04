package de.komoot.photon.nominatim;

import com.vividsolutions.jts.geom.Geometry;
import org.postgis.jts.JtsGeometry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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
            return new HashMap<>();
        }

        return map;
    }

    @Override
    public Geometry extractGeometry(ResultSet rs, String columnName) throws SQLException {
        JtsGeometry geom = (JtsGeometry) rs.getObject(columnName);
        if (geom == null) {
            return null;
        }
        return geom.getGeometry();
    }

    @Override
    public boolean hasColumn(JdbcTemplate template, String table, String column) {
        return template.query("SELECT count(*) FROM information_schema.columns WHERE table_name = ? and column_name = ?",
                new RowMapper<Boolean>() {
                    @Override
                    public Boolean mapRow(ResultSet resultSet, int i) throws SQLException {
                        return resultSet.getInt(1) > 0;
                    }
                }, table, column).get(0);
    }
}

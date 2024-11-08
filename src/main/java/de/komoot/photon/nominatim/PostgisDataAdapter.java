package de.komoot.photon.nominatim;

import net.postgis.jdbc.PGgeometry;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions to parse data from and create SQL for PostgreSQL/PostGIS.
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
        PGgeometry wkt = (PGgeometry) rs.getObject(columnName);
        if (wkt != null) {
            try {
                StringBuffer sb = new StringBuffer();
                wkt.getGeometry().outerWKT(sb);
                return new WKTReader().read(sb.toString());
            } catch (ParseException e) {
                // ignore
            }
        }

        return null;
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

    @Override
    public String deleteReturning(String deleteSQL, String columns) {
        return deleteSQL + " RETURNING " + columns;
    }

    @Override
    public String jsonArrayFromSelect(String valueSQL, String fromSQL) {
        return "(SELECT json_agg(val) FROM (SELECT " + valueSQL + " as val " + fromSQL + ") xxx)";
    }
}

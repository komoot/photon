package de.komoot.photon.nominatim;

import com.vividsolutions.jts.geom.Geometry;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Defines utility functions to parse data from the database and create SQL queries.
 */
public interface DBDataAdapter {
    /**
     * Create a hash map from the given column data.
     */
    Map<String, String> getMap(ResultSet rs, String columnName) throws SQLException;

    /**
     * Create a JTS geometry from the given column data.
     */
    Geometry extractGeometry(ResultSet rs, String columnName) throws SQLException;

    /**
     * Check if a table has the given column.
     */
    boolean hasColumn(JdbcTemplate template, String table, String column);

    /**
     * Wrap a DELETE statement with a RETURNING clause.
     */
    String deleteReturning(String deleteSQL, String columns);
}

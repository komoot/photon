package de.komoot.photon.nominatim;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Geometry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Defines utility functions to parse data from the database and create SQL queries.
 */
@NullMarked
public interface DBDataAdapter {
    /**
     * Create a hash map from the given column data.
     */
    Map<String, String> getMap(ResultSet rs, String columnName) throws SQLException;

    /**
     * Create a JTS geometry from the given column data.
     */
    @Nullable
    Geometry extractGeometry(ResultSet rs, String columnName) throws SQLException;

    /**
     * Wrap a DELETE statement with a RETURNING clause.
     */
    String deleteReturning(String deleteSQL, String columns);

    /**
     * Wrap function to create a json array from a SELECT.
     */
    String jsonArrayFromSelect(String valueSQL, String fromSQL);
}

package de.komoot.photon.nominatim;

import com.vividsolutions.jts.geom.Geometry;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Defines utility functions to parse data from the database.
 */
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
}

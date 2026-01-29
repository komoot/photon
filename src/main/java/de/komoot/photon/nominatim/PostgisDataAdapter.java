package de.komoot.photon.nominatim;

import net.postgis.jdbc.PGgeometry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions to parse data from and create SQL for PostgreSQL/PostGIS.
 */
@NullMarked
public class PostgisDataAdapter implements DBDataAdapter {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public Map<String, String> getMap(ResultSet rs, String columnName) throws SQLException {
        Map<String, String> map = (Map<String, String>) rs.getObject(columnName);
        if (map == null) {
            return new HashMap<>();
        }

        return map;
    }

    @Override
    @Nullable
    public Geometry extractGeometry(ResultSet rs, String columnName) throws SQLException {
        PGgeometry wkt = (PGgeometry) rs.getObject(columnName);
        if (wkt != null) {
            try {
                StringBuffer sb = new StringBuffer();
                wkt.getGeometry().outerWKT(sb);

                Geometry geometry = new WKTReader().read(sb.toString());
                geometry.setSRID(4326);
                return geometry;
            } catch (ParseException e) {
                // ignore
                LOGGER.error("Cannot parse database geometry", e);
            }
        }

        return null;
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

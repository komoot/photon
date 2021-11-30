package de.komoot.photon.nominatim.testdb;

import com.vividsolutions.jts.geom.Geometry;
import org.springframework.lang.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Helper functions used by H2 test database.
 * <p>
 * See test-schema.sql for how they are used.
 */
public class Helpers {

    public static Geometry envelope(Geometry geom) {
        if (geom == null)
            return null;

        return geom.getEnvelope();
    }

    public static Geometry set_srid(Geometry geom, int srid) {
        if (geom == null)
            return null;

        geom.setSRID(srid);

        return geom;
    }

    public static String[] avals(Map<String, String> input) {
        return input.values().toArray(new String[0]);
    }

    @Nullable
    public static <T extends Geometry> T extractGeometry(ResultSet rs, String columnName) throws SQLException {
        return (T) rs.getObject(columnName);
    }
}

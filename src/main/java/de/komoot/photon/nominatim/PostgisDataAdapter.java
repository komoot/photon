package de.komoot.photon.nominatim;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import org.postgis.jts.JtsGeometry;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            return Maps.newHashMap();
        }

        return map;
    }

    @Nullable
    @Override
    public Geometry extractGeometry(ResultSet rs, String columnName) throws SQLException {
        JtsGeometry geom = (JtsGeometry) rs.getObject(columnName);
        if (geom == null) {
            //info("no geometry found in column " + columnName);
            return null;
        }
        return geom.getGeometry();
    }
}

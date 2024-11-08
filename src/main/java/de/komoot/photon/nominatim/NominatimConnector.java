package de.komoot.photon.nominatim;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for workers connecting to a Nominatim database
 */
public class NominatimConnector {
    protected static final String SELECT_COLS_PLACEX = "SELECT place_id, osm_type, osm_id, class, type, name, postcode, address, extratags, ST_Envelope(geometry) AS bbox, parent_place_id, linked_place_id, rank_address, rank_search, importance, country_code, centroid";
    protected static final String SELECT_COLS_ADDRESS = "SELECT p.name, p.class, p.type, p.rank_address";
    protected static final String SELECT_OSMLINE_OLD_STYLE = "SELECT place_id, osm_id, parent_place_id, startnumber, endnumber, interpolationtype, postcode, country_code, linegeo";
    protected static final String SELECT_OSMLINE_NEW_STYLE = "SELECT place_id, osm_id, parent_place_id, startnumber, endnumber, step, postcode, country_code, linegeo";

    protected final DBDataAdapter dbutils;
    protected final JdbcTemplate template;
    protected Map<String, Map<String, String>> countryNames;
    protected final boolean hasNewStyleInterpolation;

    protected NominatimConnector(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        dataSource.setUsername(username);
        if (password != null) {
            dataSource.setPassword(password);
        }
        dataSource.setDefaultAutoCommit(true);

        template = new JdbcTemplate(dataSource);
        template.setFetchSize(100000);

        dbutils = dataAdapter;
        hasNewStyleInterpolation = dbutils.hasColumn(template, "location_property_osmline", "step");
    }

    public Date getLastImportDate() {
        List<Date> importDates = template.query("SELECT lastimportdate FROM import_status ORDER BY lastimportdate DESC LIMIT 1", new RowMapper<Date>() {
            public Date mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getTimestamp("lastimportdate");
            }
        });
        if (importDates.isEmpty()) {
            return null;
        }

        return importDates.get(0);
    }

    public void loadCountryNames() {
        if (countryNames == null) {
            countryNames = new HashMap<>();
            // Default for places outside any country.
            countryNames.put("", new HashMap<>());
            template.query("SELECT country_code, name FROM country_name", rs -> {
                countryNames.put(rs.getString("country_code"), dbutils.getMap(rs, "name"));
            });
        }
    }

}

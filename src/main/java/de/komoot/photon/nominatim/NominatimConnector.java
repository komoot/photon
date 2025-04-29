package de.komoot.photon.nominatim;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
    protected final DBDataAdapter dbutils;
    protected final JdbcTemplate template;
    protected final TransactionTemplate txTemplate;
    protected Map<String, Map<String, String>> countryNames;
    protected final boolean hasNewStyleInterpolation;
    protected boolean useGeometryColumn;

    protected NominatimConnector(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter, boolean useGeometryColumn) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        dataSource.setUsername(username);
        if (password != null) {
            dataSource.setPassword(password);
        }

        // Keep disabled or server-side cursors won't work.
        dataSource.setDefaultAutoCommit(false);

        txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        template = new JdbcTemplate(dataSource);
        template.setFetchSize(100000);

        dbutils = dataAdapter;
        hasNewStyleInterpolation = dbutils.hasColumn(template, "location_property_osmline", "step");
        this.useGeometryColumn = useGeometryColumn;
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

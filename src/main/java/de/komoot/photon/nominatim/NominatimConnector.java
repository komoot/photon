package de.komoot.photon.nominatim;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.NameMap;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for workers connecting to a Nominatim database
 */
public class NominatimConnector {
    protected final DBDataAdapter dbutils;
    protected final DatabaseProperties dbProperties;
    protected final JdbcTemplate template;
    protected final TransactionTemplate txTemplate;
    protected Map<String, NameMap> countryNames;

    protected NominatimConnector(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter, DatabaseProperties dbProperties) {
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
        this.dbProperties = dbProperties;
    }

    public Date getLastImportDate() {
        List<Date> importDates = template.query(
                "SELECT lastimportdate FROM import_status ORDER BY lastimportdate DESC LIMIT 1",
                (rs, rowNum) -> rs.getTimestamp("lastimportdate"));

        return importDates.isEmpty() ? null : importDates.get(0);
    }

    public Map<String, NameMap> loadCountryNames(String[] languages) {
        if (countryNames == null) {
            countryNames = new HashMap<>();
            // Default for places outside any country.
            countryNames.put("", new NameMap());
            template.query("SELECT country_code, name FROM country_name", rs -> {
                countryNames.put(
                        rs.getString("country_code"),
                        AddressRow.make(dbutils.getMap(rs, "name"),
                                "place",
                                "country",
                                4,
                                languages).getName());
            });
        }

        return countryNames;
    }

}

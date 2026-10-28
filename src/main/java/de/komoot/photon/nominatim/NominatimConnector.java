package de.komoot.photon.nominatim;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.config.PostgresqlConfig;
import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.model.NameMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

/**
 * Base class for workers connecting to a Nominatim database
 */
@NullMarked
public class NominatimConnector {
    protected final DBDataAdapter dbutils;
    protected final DatabaseProperties dbProperties;
    protected final JdbcTemplate template;
    protected final TransactionTemplate txTemplate;
    @Nullable protected Map<String, NameMap> countryNames;
    @Nullable private Boolean hasNewPostcodeLocationsTable;
    @Nullable private Boolean hasCategoriesColumn;

    protected NominatimConnector(PostgresqlConfig cfg, DBDataAdapter dataAdapter, DatabaseProperties dbProperties) {
        var dataSource = cfg.getDataSource();

        txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        template = new JdbcTemplate(dataSource);
        template.setFetchSize(100000);

        dbutils = dataAdapter;
        this.dbProperties = dbProperties;
    }

    @Nullable
    public Date getLastImportDate() {
        List<Date> importDates = template.query(
                "SELECT lastimportdate FROM import_status ORDER BY lastimportdate DESC LIMIT 1",
                (rs, rowNum) -> rs.getTimestamp("lastimportdate"));

        return importDates.isEmpty() ? null : importDates.getFirst();
    }

    public Map<String, NameMap> loadCountryNames(Set<String> languages) {
        if (countryNames == null) {
            countryNames = new HashMap<>();
            // Default for places outside any country.
            countryNames.put("", new NameMap());
            template.query("SELECT country_code, name FROM country_name", rs -> {
                var names = AddressRow.make(dbutils.getMap(rs, "name"),
                        "place",
                        "country",
                        AddressType.COUNTRY,
                        languages).getName();
                if (!names.isEmpty()) {
                    countryNames.put(rs.getString("country_code"), names);
                }
            });
        }

        return countryNames;
    }

    public boolean hasPostcodeTablesWithGeometry() {
        if (hasNewPostcodeLocationsTable == null) {
            var numTables = template.queryForObject("""
                    SELECT count(*) FROM information_schema.tables
                    WHERE lower(table_name) = 'location_postcodes'
                    """, Integer.class);
            hasNewPostcodeLocationsTable = numTables != null && numTables > 0;
        }

        return hasNewPostcodeLocationsTable;
    }

    public boolean hasCategoriesColumn() {
        if (hasCategoriesColumn == null) {
            var numColumns = template.queryForObject("""
                    SELECT count(*) FROM information_schema.columns
                    WHERE lower(table_name) = 'placex' AND lower(column_name) = 'categories'
                    """, Integer.class);
            hasCategoriesColumn = numColumns != null && numColumns > 0;
        }

        return hasCategoriesColumn;
    }
}

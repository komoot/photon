package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.AddressType;
import org.apache.commons.dbcp2.BasicDataSource;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Importer for data from a Nominatim database.
 */
public class NominatimConnector {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimConnector.class);

    private static final String SELECT_COLS_PLACEX = "SELECT place_id, osm_type, osm_id, class, type, name, postcode, address, extratags, ST_Envelope(geometry) AS bbox, parent_place_id, linked_place_id, rank_address, rank_search, importance, country_code, centroid";
    private static final String SELECT_COLS_ADDRESS = "SELECT p.name, p.class, p.type, p.rank_address";

    private final DBDataAdapter dbutils;
    private final JdbcTemplate template;
    private Map<String, Map<String, String>> countryNames;

    /**
     * Map a row from location_property_osmline (address interpolation lines) to a photon doc.
     * This may be old-style interpolation (using interpolationtype) or
     * new-style interpolation (using step).
     */
    private final RowMapper<NominatimResult> osmlineRowMapper;
    private final String selectOsmlineSql;


    /**
     * Maps a placex row in nominatim to a photon doc.
     * Some attributes are still missing and can be derived by connected address items.
     */
    private final RowMapper<NominatimResult> placeRowMapper = new RowMapper<>() {
        @Override
        public NominatimResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, String> address = dbutils.getMap(rs, "address");
            PhotonDoc doc = new PhotonDoc(rs.getLong("place_id"),
                                          rs.getString("osm_type"), rs.getLong("osm_id"),
                                          rs.getString("class"), rs.getString("type"))
                    .names(dbutils.getMap(rs, "name"))
                    .extraTags(dbutils.getMap(rs, "extratags"))
                    .bbox(dbutils.extractGeometry(rs, "bbox"))
                    .parentPlaceId(rs.getLong("parent_place_id"))
                    .countryCode(rs.getString("country_code"))
                    .centroid(dbutils.extractGeometry(rs, "centroid"))
                    .linkedPlaceId(rs.getLong("linked_place_id"))
                    .rankAddress(rs.getInt("rank_address"))
                    .postcode(rs.getString("postcode"));

            double importance = rs.getDouble("importance");
            doc.importance(rs.wasNull() ? (0.75 - rs.getInt("rank_search") / 40d) : importance);

            completePlace(doc);
            // Add address last, so it takes precedence.
            doc.address(address);

            doc.setCountry(countryNames.get(rs.getString("country_code")));

            NominatimResult result = new NominatimResult(doc);
            result.addHousenumbersFromAddress(address);

            return result;
        }
    };

    /**
     * Construct a new importer.
     *
     * @param host     database host
     * @param port     database port
     * @param database database name
     * @param username db username
     * @param password db username's password
     */
    public NominatimConnector(String host, int port, String database, String username, String password) {
        this(host, port, database, username, password, new PostgisDataAdapter());
    }

    public NominatimConnector(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter) {
        BasicDataSource dataSource = buildDataSource(host, port, database, username, password, true);

        template = new JdbcTemplate(dataSource);
        template.setFetchSize(100000);

        dbutils = dataAdapter;

        // Setup handling of interpolation table. There are two different formats depending on the Nominatim version.
        if (dbutils.hasColumn(template, "location_property_osmline", "step")) {
            // new-style interpolations
            selectOsmlineSql = "SELECT place_id, osm_id, parent_place_id, startnumber, endnumber, step, postcode, country_code, linegeo";
            osmlineRowMapper = (rs, rownum) -> {
                Geometry geometry = dbutils.extractGeometry(rs, "linegeo");

                PhotonDoc doc = new PhotonDoc(rs.getLong("place_id"), "W", rs.getLong("osm_id"),
                        "place", "house_number")
                        .parentPlaceId(rs.getLong("parent_place_id"))
                        .countryCode(rs.getString("country_code"))
                        .postcode(rs.getString("postcode"));

                completePlace(doc);

                doc.setCountry(countryNames.get(rs.getString("country_code")));

                NominatimResult result = new NominatimResult(doc);
                result.addHouseNumbersFromInterpolation(rs.getLong("startnumber"), rs.getLong("endnumber"),
                        rs.getLong("step"), geometry);

                return result;
            };
        } else {
            // old-style interpolations
            selectOsmlineSql = "SELECT place_id, osm_id, parent_place_id, startnumber, endnumber, interpolationtype, postcode, country_code, linegeo";
            osmlineRowMapper = (rs, rownum) -> {
                Geometry geometry = dbutils.extractGeometry(rs, "linegeo");

                PhotonDoc doc = new PhotonDoc(rs.getLong("place_id"), "W", rs.getLong("osm_id"),
                        "place", "house_number")
                        .parentPlaceId(rs.getLong("parent_place_id"))
                        .countryCode(rs.getString("country_code"))
                        .postcode(rs.getString("postcode"));

                completePlace(doc);

                doc.setCountry(countryNames.get(rs.getString("country_code")));

                NominatimResult result = new NominatimResult(doc);
                result.addHouseNumbersFromInterpolation(rs.getLong("startnumber"), rs.getLong("endnumber"),
                        rs.getString("interpolationtype"), geometry);

                return result;
            };
        }
    }


    static BasicDataSource buildDataSource(String host, int port, String database, String username, String password, boolean autocommit) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        dataSource.setUsername(username);
        if (password != null) {
            dataSource.setPassword(password);
        }
        dataSource.setDefaultAutoCommit(autocommit);
        return dataSource;
    }

    public void loadCountryNames() {
        if (countryNames == null) {
            countryNames = new HashMap<>();
            template.query("SELECT country_code, name FROM country_name", rs -> {
                countryNames.put(rs.getString("country_code"), dbutils.getMap(rs, "name"));
            });
        }
    }


    public List<PhotonDoc> getByPlaceId(long placeId) {
        List<NominatimResult> result = template.query(SELECT_COLS_PLACEX + " FROM placex WHERE place_id = ? and indexed_status = 0",
                                                         placeRowMapper, placeId);

        return result.isEmpty() ? null : result.get(0).getDocsWithHousenumber();
    }

    public List<PhotonDoc> getInterpolationsByPlaceId(long placeId) {
        List<NominatimResult> result = template.query(selectOsmlineSql
                                                          + " FROM location_property_osmline WHERE place_id = ? and indexed_status = 0",
                                                          osmlineRowMapper, placeId);

        return result.isEmpty() ? null : result.get(0).getDocsWithHousenumber();
    }

    private long parentPlaceId = -1;
    private List<AddressRow> parentTerms = null;

    List<AddressRow> getAddresses(PhotonDoc doc) {
        RowMapper<AddressRow> rowMapper = (rs, rowNum) -> new AddressRow(
                dbutils.getMap(rs, "name"),
                rs.getString("class"),
                rs.getString("type"),
                rs.getInt("rank_address")
        );

        AddressType atype = doc.getAddressType();

        if (atype == null || atype == AddressType.COUNTRY) {
            return Collections.emptyList();
        }

        List<AddressRow> terms = null;

        if (atype == AddressType.HOUSE) {
            long placeId = doc.getParentPlaceId();
            if (placeId != parentPlaceId) {
                parentTerms = template.query(SELECT_COLS_ADDRESS
                                + " FROM placex p, place_addressline pa"
                                + " WHERE p.place_id = pa.address_place_id and pa.place_id = ?"
                                + " and pa.cached_rank_address > 4 and pa.address_place_id != ? and pa.isaddress"
                                + " ORDER BY rank_address desc, fromarea desc, distance asc, rank_search desc",
                        rowMapper, placeId, placeId);

                // need to add the term for the parent place ID itself
                parentTerms.addAll(0, template.query(SELECT_COLS_ADDRESS + " FROM placex p WHERE p.place_id = ?",
                        rowMapper, placeId));
                parentPlaceId = placeId;
            }
            terms = parentTerms;

        } else {
            long placeId = doc.getPlaceId();
            terms = template.query(SELECT_COLS_ADDRESS
                            + " FROM placex p, place_addressline pa"
                            + " WHERE p.place_id = pa.address_place_id and pa.place_id = ?"
                            + " and pa.cached_rank_address > 4 and pa.address_place_id != ? and pa.isaddress"
                            + " ORDER BY rank_address desc, fromarea desc, distance asc, rank_search desc",
                    rowMapper, placeId, placeId);
        }

        return terms;
    }

    /**
     * Parse every relevant row in placex and location_osmline
     * for the given country. Also imports place from county-less places.
     */
    public void readCountry(String countryCode, ImportThread importThread) {
        // Make sure, country names are available.
        loadCountryNames();
        if (countryCode != null && !countryNames.containsKey(countryCode)) {
            LOGGER.warn("Unknown country code {}. Skipping.", countryCode);
            return;
        }

        final RowCallbackHandler placeMapper = rs -> {
                NominatimResult docs = placeRowMapper.mapRow(rs, 0);
                assert (docs != null);

                if (docs.isUsefulForIndex()) {
                    importThread.addDocument(docs);
                }
            };

        final RowCallbackHandler osmlineMapper = rs -> {
            NominatimResult docs = osmlineRowMapper.mapRow(rs, 0);
            assert (docs != null);

            if (docs.isUsefulForIndex()) {
                importThread.addDocument(docs);
            }
        };

        if (countryCode == null) {
            template.query(SELECT_COLS_PLACEX + " FROM placex " +
                    " WHERE linked_place_id IS NULL AND centroid IS NOT NULL AND country_code is null" +
                    " ORDER BY geometry_sector, parent_place_id; ", placeMapper);

            template.query(selectOsmlineSql + " FROM location_property_osmline " +
                    "WHERE startnumber is not null AND country_code is null " +
                    " ORDER BY geometry_sector, parent_place_id; ", osmlineMapper);
        } else {
            template.query(SELECT_COLS_PLACEX + " FROM placex " +
                    " WHERE linked_place_id IS NULL AND centroid IS NOT NULL AND country_code = ?" +
                    " ORDER BY geometry_sector, parent_place_id; ", placeMapper, countryCode);

            template.query(selectOsmlineSql + " FROM location_property_osmline " +
                    "WHERE startnumber is not null AND country_code = ?" +
                    " ORDER BY geometry_sector, parent_place_id; ", osmlineMapper, countryCode);

        }
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

    /**
     * Query Nominatim's address hierarchy to complete photon doc with missing data (like country, city, street, ...)
     *
     * @param doc
     */
    private void completePlace(PhotonDoc doc) {
        final List<AddressRow> addresses = getAddresses(doc);
        final AddressType doctype = doc.getAddressType();
        for (AddressRow address : addresses) {
            AddressType atype = address.getAddressType();

            if (atype != null
                    && (atype == doctype || !doc.setAddressPartIfNew(atype, address.getName()))
                    && address.isUsefulForContext()) {
                // no specifically handled item, check if useful for context
                doc.getContext().add(address.getName());
            }
        }
    }

    public DBDataAdapter getDataAdaptor() {
        return dbutils;
    }

    /**
     * Prepare the database for export.
     *
     * This function ensures that the proper index are available and if
     * not will create them. This may take a while.
     */
    public void prepareDatabase() {
        Integer indexRowNum = template.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE tablename = 'placex' AND indexdef LIKE '%(country_code)'",
                Integer.class);

        if (indexRowNum == null || indexRowNum == 0) {
            LOGGER.info("Creating index over countries.");
            template.execute("CREATE INDEX ON placex (country_code)");
        }
    }

    public String[] getCountriesFromDatabase() {
        loadCountryNames();
        String[] countries = new String[countryNames.keySet().size() + 1];
        countries[0] = null;

        int i = 1;
        for (var country: countryNames.keySet()) {
            countries[i++] = country;
        }

        return countries;
    }
}

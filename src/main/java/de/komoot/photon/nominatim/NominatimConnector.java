package de.komoot.photon.nominatim;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.AddressType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.postgis.jts.JtsWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Export nominatim data
 *
 * @author felix, christoph
 */
@Slf4j
public class NominatimConnector {
    private static final String SELECT_COLS_PLACEX = "SELECT place_id, osm_type, osm_id, class, type, name, postcode, address, extratags, ST_Envelope(geometry) AS bbox, parent_place_id, linked_place_id, rank_address, rank_search, importance, country_code, centroid";
    private static final String SELECT_COLS_OSMLINE = "SELECT place_id, osm_id, parent_place_id, startnumber, endnumber, interpolationtype, postcode, country_code, address, linegeo";
    private static final String SELECT_COLS_ADDRESS = "SELECT p.name, p.class, p.type, p.rank_address";

    private final DBDataAdapter dbutils;
    private final JdbcTemplate template;
    private Map<String, Map<String, String>> countryNames;
    /**
     * Maps a row from location_property_osmline (address interpolation lines) to a photon doc.
     */
    private final RowMapper<NominatimResult> osmlineRowMapper = new RowMapper<NominatimResult>() {
        @Override
        public NominatimResult mapRow(ResultSet rs, int rownum) throws SQLException {
            Geometry geometry = dbutils.extractGeometry(rs, "linegeo");
            Map<String, String> address = dbutils.getMap(rs, "address");

            PhotonDoc doc = new PhotonDoc(rs.getLong("place_id"), "W", rs.getLong("osm_id"),
                                          "place", "house_number")
                    .parentPlaceId(rs.getLong("parent_place_id"))
                    .countryCode(rs.getString("country_code"))
                    .postcode(rs.getString("postcode"));

            completePlace(doc, address);

            doc.setCountry(getCountryNames(rs.getString("country_code")));

            NominatimResult result = new NominatimResult(doc);
            result.addHouseNumbersFromInterpolation(rs.getLong("startnumber"), rs.getLong("endnumber"),
                    rs.getString("interpolationtype"), geometry);

            return result;
        }
    };
    /**
     * maps a placex row in nominatim to a photon doc, some attributes are still missing and can be derived by connected address items.
     */
    private final RowMapper<NominatimResult> placeRowMapper = new RowMapper<NominatimResult>() {
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

            completePlace(doc, address);
            // Add address last, so it takes precedence.
            doc.address(address);

            doc.setCountry(getCountryNames(rs.getString("country_code")));

            NominatimResult result = new NominatimResult(doc);
            result.addHousenumbersFromAddress(address);

            return result;
        }
    };
    private Importer importer;

    /**
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
        BasicDataSource dataSource = buildDataSource(host, port, database, username, password, false);

        template = new JdbcTemplate(dataSource);
        template.setFetchSize(100000);

        dbutils = dataAdapter;
    }


    static BasicDataSource buildDataSource(String host, int port, String database, String username, String password, boolean autocommit) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl(String.format("jdbc:postgres_jts://%s:%d/%s", host, port, database));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(JtsWrapper.class.getCanonicalName());
        dataSource.setDefaultAutoCommit(autocommit);
        return dataSource;
    }

    private Map<String, String> getCountryNames(String countrycode) {
        if (countryNames == null) {
            countryNames = new HashMap<>();
            template.query("SELECT country_code, name FROM country_name", rs -> {
                countryNames.put(rs.getString("country_code"), dbutils.getMap(rs, "name"));
            });
        }

        return countryNames.get(countrycode);
    }

    public void setImporter(Importer importer) {
        this.importer = importer;
    }

    public List<PhotonDoc> getByPlaceId(long placeId) {
        NominatimResult result = template.queryForObject(SELECT_COLS_PLACEX + " FROM placex WHERE place_id = ?",
                                                         placeRowMapper, placeId);
        assert(result != null);
        return result.getDocsWithHousenumber();
    }

    public List<PhotonDoc> getInterpolationsByPlaceId(long placeId) {
        NominatimResult result = template.queryForObject(SELECT_COLS_OSMLINE
                                                          + " FROM location_property_osmline WHERE place_id = ?",
                                                          osmlineRowMapper, placeId);
        assert(result != null);
        return result.getDocsWithHousenumber();
    }

    List<AddressRow> getAddresses(PhotonDoc doc, Map<String, String> addressTags) {
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

        if (atype != AddressType.HOUSE) {
            long placeId = doc.getPlaceId();
            return template.query(SELECT_COLS_ADDRESS
                            + " FROM placex p, place_addressline pa"
                            + " WHERE p.place_id = pa.address_place_id and pa.place_id = ?"
                            + " and pa.cached_rank_address > 4 and pa.address_place_id != ? and pa.isaddress"
                            + " ORDER BY rank_address desc, fromarea desc, distance asc, rank_search desc",
                    rowMapper, placeId, placeId);
        }

        // Corresponds to Nominatim's address computation code in lib-sql/functions/address_lookup.sql
        // This corrects for switching boundaries.
        long placeId = doc.getPlaceId();
        Point centroid = doc.getCentroid();
        List<Object> args = new ArrayList<Object>();

        args.add(placeId);
        args.add(doc.getParentPlaceId());
        args.add(placeId);
        if (addressTags != null) {
            args.add(addressTags.values().toArray(new String[0]));
        }
        if (centroid != null) {
            args.add(centroid.toText());
        }

        return template.query(dbutils.addressSQL(addressTags != null, centroid != null),
                              args.toArray(), rowMapper);
    }

    static String convertCountryCode(String... countryCodes) {
        String countryCodeStr = "";
        for (String cc : countryCodes) {
            // "".split(",") results in 'new String[]{""}' and not 'new String[0]'
            if (cc.isEmpty())
                continue;
            if (cc.length() != 2)
                throw new IllegalArgumentException("country code invalid " + cc);
            if (!countryCodeStr.isEmpty())
                countryCodeStr += ",";
            countryCodeStr += "'" + cc.toLowerCase() + "'";
        }
        return countryCodeStr;
    }

    /**
     * parses every relevant row in placex, creates a corresponding document and calls the {@link #importer} for every document
     */
    public void readEntireDatabase(String... countryCodes) {
        String andCountryCodeStr = "";
        String whereCountryCodeStr = "";
        String countryCodeStr = convertCountryCode(countryCodes);
        if (!countryCodeStr.isEmpty()) {
            andCountryCodeStr = "AND country_code in (" + countryCodeStr + ")";
            whereCountryCodeStr = "WHERE country_code in (" + countryCodeStr + ")";
        }

        log.info("start importing documents from nominatim (" + (countryCodeStr.isEmpty() ? "global" : countryCodeStr) + ")");

        ImportThread importThread = new ImportThread(importer);

        template.query(SELECT_COLS_PLACEX + " FROM placex " +
                " WHERE linked_place_id IS NULL AND centroid IS NOT NULL " + andCountryCodeStr +
                " ORDER BY geometry_sector, parent_place_id; ", rs -> {
                    // turns a placex row into a photon document that gathers all de-normalised information
                    NominatimResult docs = placeRowMapper.mapRow(rs, 0);
                    assert(docs != null);

                    if (docs.isUsefulForIndex()) {
                        importThread.addDocument(docs);
                    }
                });

        template.query(SELECT_COLS_OSMLINE + " FROM location_property_osmline " +
                whereCountryCodeStr +
                " ORDER BY geometry_sector, parent_place_id; ", rs -> {
                    NominatimResult docs = osmlineRowMapper.mapRow(rs, 0);
                    assert(docs != null);

                    if (docs.isUsefulForIndex()) {
                        importThread.addDocument(docs);
                    }
                });

        importThread.finish();
    }

    /**
     * querying nominatim's address hierarchy to complete photon doc with missing data (like country, city, street, ...)
     *
     * @param doc
     */
    private void completePlace(PhotonDoc doc, Map<String, String> addressTags) {
        final List<AddressRow> addresses = getAddresses(doc, addressTags);
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
}

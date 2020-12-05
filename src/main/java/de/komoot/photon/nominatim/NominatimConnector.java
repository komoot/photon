package de.komoot.photon.nominatim;

import com.vividsolutions.jts.geom.Envelope;
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
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Export nominatim data
 *
 * @author felix, christoph
 */
@Slf4j
public class NominatimConnector {
    private static final PhotonDoc FINAL_DOCUMENT = new PhotonDoc(0, null, 0, null, null, null, null, null, null, null, 0, 0, null, null, 0, 0);

    private static final String SELECT_COLS_PLACEX = "SELECT place_id, osm_type, osm_id, class, type, name, housenumber, postcode, address, extratags, ST_Envelope(geometry) AS bbox, parent_place_id, linked_place_id, rank_address, rank_search, importance, country_code, centroid";
    private static final String SELECT_COLS_OSMLINE = "SELECT place_id, osm_id, parent_place_id, startnumber, endnumber, interpolationtype, postcode, country_code, linegeo";
    private static final String SELECT_COLS_ADDRESS = "SELECT p.place_id, p.name, p.class, p.type, p.rank_address";

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

            PhotonDoc doc = new PhotonDoc(
                    rs.getLong("place_id"),
                    "W",
                    rs.getLong("osm_id"),
                    "place",
                    "house_number",
                    Collections.emptyMap(), // no name
                    null,
                    Collections.emptyMap(), // no address
                    Collections.emptyMap(), // no extratags
                    null,
                    rs.getLong("parent_place_id"),
                    0d, // importance
                    rs.getString("country_code"),
                    null, // centroid
                    0,
                    30
            );
            doc.setPostcode(rs.getString("postcode"));
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

            Double importance = rs.getDouble("importance");
            if (rs.wasNull()) {
                // https://github.com/komoot/photon/issues/12
                int rankSearch = rs.getInt("rank_search");
                importance = 0.75 - rankSearch / 40d;
            }

            Geometry geometry = dbutils.extractGeometry(rs, "bbox");
            Envelope envelope = geometry != null ? geometry.getEnvelopeInternal() : null;

            PhotonDoc doc = new PhotonDoc(
                    rs.getLong("place_id"),
                    rs.getString("osm_type"),
                    rs.getLong("osm_id"),
                    rs.getString("class"),
                    rs.getString("type"),
                    dbutils.getMap(rs, "name"),
                    null,
                    dbutils.getMap(rs, "address"),
                    dbutils.getMap(rs, "extratags"),
                    envelope,
                    rs.getLong("parent_place_id"),
                    importance,
                    rs.getString("country_code"),
                    (Point) dbutils.extractGeometry(rs, "centroid"),
                    rs.getLong("linked_place_id"),
                    rs.getInt("rank_address")
            );

            doc.setPostcode(rs.getString("postcode"));
            doc.setCountry(getCountryNames(rs.getString("country_code")));

            NominatimResult result = new NominatimResult(doc);
            result.addHousenumbersFromString(rs.getString("housenumber"));

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
            template.query("SELECT country_code, name FROM country_name;", rs -> {
                        countryNames.put(rs.getString("country_code"), dbutils.getMap(rs, "name"));
                    }
            );
        }

        return countryNames.get(countrycode);
    }

    public void setImporter(Importer importer) {
        this.importer = importer;
    }

    public List<PhotonDoc> getByPlaceId(long placeId) {
        NominatimResult result = template.queryForObject(SELECT_COLS_PLACEX + " FROM placex WHERE place_id = ?",
                                                         placeRowMapper, placeId);
        completePlace(result.getBaseDoc());
        return result.getDocsWithHousenumber();
    }

    public List<PhotonDoc> getInterpolationsByPlaceId(long placeId) {
        NominatimResult result = template.queryForObject(SELECT_COLS_OSMLINE
                                                          + " FROM location_property_osmline WHERE place_id = ?",
                                                          osmlineRowMapper, placeId);
        completePlace(result.getBaseDoc());
        return result.getDocsWithHousenumber();
    }

    List<AddressRow> getAddresses(PhotonDoc doc) {
        RowMapper<AddressRow> rowMapper = (rs, rowNum) -> new AddressRow(
                rs.getLong("place_id"),
                dbutils.getMap(rs, "name"),
                rs.getString("class"),
                rs.getString("type"),
                rs.getInt("rank_address")
        );

        AddressType atype = doc.getAddressType();

        if (atype == null || atype == AddressType.COUNTRY) {
            return Collections.emptyList();
        }

        long placeId = (atype == AddressType.HOUSE) ? doc.getParentPlaceId() : doc.getPlaceId();

        List<AddressRow> terms = template.query(SELECT_COLS_ADDRESS
                        + " FROM placex p, place_addressline pa"
                        + " WHERE p.place_id = pa.address_place_id and pa.place_id = ?"
                        + " and pa.cached_rank_address > 4 and pa.address_place_id != ? and pa.isaddress"
                        + " ORDER BY rank_address desc, fromarea desc, distance asc, rank_search desc",
                rowMapper, placeId, placeId);

        if (atype == AddressType.HOUSE) {
            // need to add the term for the parent place ID itself
            terms.addAll(0, template.query(SELECT_COLS_ADDRESS + " FROM placex p WHERE p.place_id = ?",
                    rowMapper, placeId));
        }

        return terms;
    }

    private class ImportThread implements Runnable {
        private final BlockingQueue<PhotonDoc> documents;

        public ImportThread(BlockingQueue<PhotonDoc> documents) {
            this.documents = documents;
        }

        @Override
        public void run() {
            while (true) {
                PhotonDoc doc;
                try {
                    doc = documents.take();
                    if (doc == FINAL_DOCUMENT)
                        break;
                    importer.add(doc);
                } catch (InterruptedException e) {
                    log.info("interrupted exception ", e);
                }
            }
            importer.finish();
        }
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
        final int progressInterval = 50000;
        final long startMillis = System.currentTimeMillis();

        String andCountryCodeStr = "", whereCountryCodeStr = "";
        String countryCodeStr = convertCountryCode(countryCodes);
        if (!countryCodeStr.isEmpty()) {
            andCountryCodeStr = "AND country_code in (" + countryCodeStr + ")";
            whereCountryCodeStr = "WHERE country_code in (" + countryCodeStr + ")";
        }

        log.info("start importing documents from nominatim (" + (countryCodeStr.isEmpty() ? "global" : countryCodeStr) + ")");

        final BlockingQueue<PhotonDoc> documents = new LinkedBlockingDeque<>(20);
        Thread importThread = new Thread(new ImportThread(documents));
        importThread.start();
        final AtomicLong counter = new AtomicLong();
        template.query(SELECT_COLS_PLACEX +
                " FROM placex " +
                " WHERE linked_place_id IS NULL AND centroid IS NOT NULL " + andCountryCodeStr +
                " ORDER BY geometry_sector; ", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                // turns a placex row into a photon document that gathers all de-normalised information

                NominatimResult docs = placeRowMapper.mapRow(rs, 0);

                if (!docs.isUsefulForIndex()) return; // do not import document

                // finalize document by taking into account the higher level placex rows assigned to this row
                completePlace(docs.getBaseDoc());

                for (PhotonDoc doc : docs.getDocsWithHousenumber()) {
                    while (true) {
                        try {
                            documents.put(doc);
                        } catch (InterruptedException e) {
                            log.warn("Thread interrupted while placing document in queue.");
                            continue;
                        }
                        break;
                    }
                    if (counter.incrementAndGet() % progressInterval == 0) {
                        final double documentsPerSecond = 1000d * counter.longValue() / (System.currentTimeMillis() - startMillis);
                        log.info(String.format("imported %s documents [%.1f/second]", MessageFormat.format("{0}", counter.longValue()), documentsPerSecond));
                    }
                }
            }
        });

        template.query(SELECT_COLS_OSMLINE +
                " FROM location_property_osmline " +
                whereCountryCodeStr +
                " ORDER BY geometry_sector; ", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                NominatimResult docs = osmlineRowMapper.mapRow(rs, 0);

                if (!docs.isUsefulForIndex()) return; // do not import document

                // finalize document by taking into account the higher level placex rows assigned to this row
                completePlace(docs.getBaseDoc());

                for (PhotonDoc doc : docs.getDocsWithHousenumber()) {
                    while (true) {
                        try {
                            documents.put(doc);
                        } catch (InterruptedException e) {
                            log.warn("Thread interrupted while placing document in queue.");
                            continue;
                        }
                        break;
                    }
                    if (counter.incrementAndGet() % progressInterval == 0) {
                        final double documentsPerSecond = 1000d * counter.longValue() / (System.currentTimeMillis() - startMillis);
                        log.info(String.format("imported %s documents [%.1f/second]", MessageFormat.format("{0}", counter.longValue()), documentsPerSecond));
                    }
                }
            }
        });

        while (true) {
            try {
                documents.put(FINAL_DOCUMENT);
                importThread.join();
            } catch (InterruptedException e) {
                log.warn("Thread interrupted while placing document in queue.");
                continue;
            }
            break;
        }
        log.info(String.format("finished import of %s photon documents.", MessageFormat.format("{0}", counter.longValue())));
    }

    /**
     * querying nominatim's address hierarchy to complete photon doc with missing data (like country, city, street, ...)
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
        // finally, overwrite gathered information with higher prio
        // address info from nominatim which should have precedence
        doc.completeFromAddress();
    }
}

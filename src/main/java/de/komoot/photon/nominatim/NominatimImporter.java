package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.model.OsmlineRowMapper;
import de.komoot.photon.nominatim.model.PlaceRowMapper;
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
public class NominatimImporter extends NominatimConnector {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimImporter.class);

    // One-item cache for address lookup. Speeds up rank 30 processing.
    private long parentPlaceId = -1;
    private List<AddressRow> parentTerms = null;

    public NominatimImporter(String host, int port, String database, String username, String password) {
        this(host, port, database, username, password, new PostgisDataAdapter());
    }

    public NominatimImporter(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter) {
        super(host, port, database, username, password, dataAdapter);
    }


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
        final var cnames = countryNames.get(countryCode);
        if (cnames == null) {
            LOGGER.warn("Unknown country code {}. Skipping.", countryCode);
            return;
        }

        final PlaceRowMapper placeRowMapper = new PlaceRowMapper(dbutils);
        final RowCallbackHandler placeMapper = rs -> {
                final PhotonDoc doc = placeRowMapper.mapRow(rs, 0);
                assert (doc != null);

                final Map<String, String> address = dbutils.getMap(rs, "address");


                completePlace(doc);
                // Add address last, so it takes precedence.
                doc.address(address);

                doc.setCountry(cnames);

                var result = NominatimResult.fromAddress(doc, address);

                if (result.isUsefulForIndex()) {
                    importThread.addDocument(result);
                }
            };

        final OsmlineRowMapper osmlineRowMapper = new OsmlineRowMapper();
        final RowCallbackHandler osmlineMapper = rs -> {
            final PhotonDoc doc = osmlineRowMapper.mapRow(rs, 0);

            completePlace(doc);
            doc.setCountry(cnames);

            final Geometry geometry = dbutils.extractGeometry(rs, "linegeo");
            final NominatimResult docs;
            if (hasNewStyleInterpolation) {
                docs = NominatimResult.fromInterpolation(
                        doc, rs.getLong("startnumber"), rs.getLong("endnumber"),
                        rs.getLong("step"), geometry);
            } else {
                docs = NominatimResult.fromInterpolation(
                        doc, rs.getLong("startnumber"), rs.getLong("endnumber"),
                        rs.getString("interpolationtype"), geometry);
            }

            if (docs.isUsefulForIndex()) {
                importThread.addDocument(docs);
            }
        };

        if ("".equals(countryCode)) {
            template.query(SELECT_COLS_PLACEX + " FROM placex " +
                    " WHERE linked_place_id IS NULL AND centroid IS NOT NULL AND country_code is null" +
                    " ORDER BY geometry_sector, parent_place_id; ", placeMapper);

            template.query((hasNewStyleInterpolation ? SELECT_OSMLINE_NEW_STYLE : SELECT_OSMLINE_OLD_STYLE) +
                    " FROM location_property_osmline" +
                    " WHERE startnumber is not null AND country_code is null" +
                    " ORDER BY geometry_sector, parent_place_id; ", osmlineMapper);
        } else {
            template.query(SELECT_COLS_PLACEX + " FROM placex " +
                    " WHERE linked_place_id IS NULL AND centroid IS NOT NULL AND country_code = ?" +
                    " ORDER BY geometry_sector, parent_place_id; ", placeMapper, countryCode);

            template.query((hasNewStyleInterpolation ? SELECT_OSMLINE_NEW_STYLE : SELECT_OSMLINE_OLD_STYLE) +
                    " FROM location_property_osmline" +
                    " WHERE startnumber is not null AND country_code = ?" +
                    " ORDER BY geometry_sector, parent_place_id; ", osmlineMapper, countryCode);

        }
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

        return countryNames.keySet().toArray(new String[0]);
    }
}

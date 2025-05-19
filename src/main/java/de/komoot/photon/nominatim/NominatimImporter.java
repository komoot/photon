package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.PhotonDocAddressSet;
import de.komoot.photon.PhotonDocInterpolationSet;
import de.komoot.photon.nominatim.model.AddressRow;
import de.komoot.photon.nominatim.model.NominatimAddressCache;
import de.komoot.photon.nominatim.model.OsmlineRowMapper;
import de.komoot.photon.nominatim.model.PlaceRowMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Importer for data from a Nominatim database.
 */
public class NominatimImporter extends NominatimConnector {
    private static final Logger LOGGER = LogManager.getLogger();

    public NominatimImporter(String host, int port, String database, String username, String password, boolean useGeometryColumn) {
        this(host, port, database, username, password, new PostgisDataAdapter(), useGeometryColumn);
    }

    public NominatimImporter(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter, boolean useGeometryColumn) {
        super(host, port, database, username, password, dataAdapter, useGeometryColumn);
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

        final String countrySQL;
        final Object[] sqlArgs;
        final int[] sqlArgTypes;
        if ("".equals(countryCode)) {
            countrySQL = "country_code is null";
            sqlArgs = new Object[0];
            sqlArgTypes = new int[0];
        } else {
            countrySQL = "country_code = ?";
            sqlArgs = new Object[]{countryCode};
            sqlArgTypes = new int[]{Types.VARCHAR};
        }

        NominatimAddressCache addressCache = new NominatimAddressCache(dbutils);
        addressCache.loadCountryAddresses(template, countryCode);

        final PlaceRowMapper placeRowMapper = new PlaceRowMapper(dbutils, useGeometryColumn);
        final String baseSelect = placeRowMapper.makeBaseSelect();

        // First read ranks below 30, independent places
        template.query(
                baseSelect +
                        " FROM placex p" +
                        " WHERE linked_place_id IS NULL AND centroid IS NOT NULL AND " + countrySQL +
                        " AND rank_search < 30" +
                        " ORDER BY geometry_sector, parent_place_id",
                sqlArgs, sqlArgTypes, rs -> {
                    final PhotonDoc doc = placeRowMapper.mapRow(rs, 0);
                    final Map<String, String> address = dbutils.getMap(rs, "address");

                    assert (doc != null);

                    doc.completePlace(addressCache.getAddressList(rs.getString("addresslines")));
                    doc.address(address); // take precedence over computed address
                    doc.setCountry(cnames);

                    importThread.addDocument(new PhotonDocAddressSet(doc, address));
                });

        // Next get all POIs/housenumbers.
        template.query(
                 baseSelect +
                         " , parent.class as parent_class, parent.type as parent_type," +
                         "   parent.rank_address as parent_rank_address, parent.name as parent_name" +
                        " FROM placex p LEFT JOIN placex parent ON p.parent_place_id = parent.place_id" +
                        " WHERE p.linked_place_id IS NULL AND p.centroid IS NOT NULL AND p." + countrySQL +
                        " AND p.rank_search = 30 " +
                        " ORDER BY p.geometry_sector",
                sqlArgs, sqlArgTypes, rs -> {
                    final PhotonDoc doc = placeRowMapper.mapRow(rs, 0);
                    final Map<String, String> address = dbutils.getMap(rs, "address");

                    assert (doc != null);

                    if (rs.getString("parent_class") != null) {
                        doc.completePlace(List.of(new AddressRow(
                                dbutils.getMap(rs, "parent_name"),
                                rs.getString("parent_class"),
                                rs.getString("parent_type"),
                                rs.getInt("parent_rank_address"))));
                    }
                    doc.completePlace(addressCache.getAddressList(rs.getString("addresslines")));
                    doc.address(address); // take precedence over computed address
                    doc.setCountry(cnames);

                    importThread.addDocument(new PhotonDocAddressSet(doc, address));
                });

        final OsmlineRowMapper osmlineRowMapper = new OsmlineRowMapper();
        template.query(String.format("%s  AND p.%s ORDER BY p.geometry_sector, p.parent_place_id",
                                     osmlineRowMapper.makeBaseQuery(dbutils), countrySQL),
                sqlArgs, sqlArgTypes, rs -> {
                    final PhotonDoc doc = osmlineRowMapper.mapRow(rs, 0);

                    if (rs.getString("parent_class") != null) {
                        doc.completePlace(List.of(new AddressRow(
                                dbutils.getMap(rs, "parent_name"),
                                rs.getString("parent_class"),
                                rs.getString("parent_type"),
                                rs.getInt("parent_rank_address"))));
                    }
                    doc.completePlace(addressCache.getAddressList(rs.getString("addresslines")));
                    doc.address(dbutils.getMap(rs, "address"));

                    doc.setCountry(cnames);

                    importThread.addDocument(new PhotonDocInterpolationSet(
                            doc,
                            rs.getLong("startnumber"),
                            rs.getLong("endnumber"),
                            rs.getLong("step"),
                            dbutils.extractGeometry(rs, "linegeo")
                    ));
                });

    }


    /**
     * Prepare the database for export.
     *
     * This function ensures that the proper index are available and if
     * not will create them. This may take a while.
     */
    public void prepareDatabase() {
        txTemplate.execute(status -> {
            Integer indexRowNum = template.queryForObject(
                    "SELECT count(*) FROM pg_indexes WHERE tablename = 'placex' AND indexdef LIKE '%(country_code)'",
                    Integer.class);

            if (indexRowNum == null || indexRowNum == 0) {
                LOGGER.info("Creating index over countries.");
                template.execute("CREATE INDEX ON placex (country_code)");
            }

            return 0;
        });
    }

    public String[] getCountriesFromDatabase() {
        loadCountryNames();

        return countryNames.keySet().toArray(new String[0]);
    }
}

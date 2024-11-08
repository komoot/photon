package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.*;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;

import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Importer for data from a Nominatim database.
 */
public class NominatimImporter extends NominatimConnector {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimImporter.class);

    public NominatimImporter(String host, int port, String database, String username, String password) {
        this(host, port, database, username, password, new PostgisDataAdapter());
    }

    public NominatimImporter(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter) {
        super(host, port, database, username, password, dataAdapter);
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

        NominatimAddressCache addressCache = new NominatimAddressCache();
        addressCache.loadCountryAddresses(template, dbutils, countryCode);

        final PlaceRowMapper placeRowMapper = new PlaceRowMapper(dbutils);
        // First read ranks below 30, independent places
        template.query(
                "SELECT place_id, osm_type, osm_id, class, type, name, postcode," +
                        "       address, extratags, ST_Envelope(geometry) AS bbox, parent_place_id," +
                        "       linked_place_id, rank_address, rank_search, importance, country_code, centroid," +
                        dbutils.jsonArrayFromSelect(
                                "address_place_id",
                                "FROM place_addressline pa " +
                                        " WHERE pa.place_id = p.place_id AND isaddress" +
                                        " ORDER BY cached_rank_address DESC") + " as addresslines" +
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

                    var result = NominatimResult.fromAddress(doc, address);

                    if (result.isUsefulForIndex()) {
                        importThread.addDocument(result);
                    }
                });

        // Next get all POIs/housenumbers.
        template.query(
                "SELECT p.place_id, p.osm_type, p.osm_id, p.class, p.type, p.name, p.postcode," +
                        "       p.address, p.extratags, ST_Envelope(p.geometry) AS bbox, p.parent_place_id," +
                        "       p.linked_place_id, p.rank_address, p.rank_search, p.importance, p.country_code, p.centroid," +
                        "       parent.class as parent_class, parent.type as parent_type," +
                        "       parent.rank_address as parent_rank_address, parent.name as parent_name, " +
                        dbutils.jsonArrayFromSelect(
                                "address_place_id",
                                "FROM place_addressline pa " +
                                        " WHERE pa.place_id IN (p.place_id, coalesce(p.parent_place_id, p.place_id)) AND isaddress" +
                                        " ORDER BY cached_rank_address DESC, pa.place_id = p.place_id DESC") + " as addresslines" +
                        " FROM placex p LEFT JOIN placex parent ON p.parent_place_id = parent.place_id" +
                        " WHERE p.linked_place_id IS NULL AND p.centroid IS NOT NULL AND p." + countrySQL +
                        " AND p.rank_search = 30 " +
                        " ORDER BY p.geometry_sector",
                sqlArgs, sqlArgTypes, rs -> {
                    final PhotonDoc doc = placeRowMapper.mapRow(rs, 0);
                    final Map<String, String> address = dbutils.getMap(rs, "address");

                    assert (doc != null);

                    final var addressPlaces = addressCache.getAddressList(rs.getString("addresslines"));
                    if (rs.getString("parent_class") != null) {
                        addressPlaces.add(0, new AddressRow(
                                dbutils.getMap(rs, "parent_name"),
                                rs.getString("parent_class"),
                                rs.getString("parent_type"),
                                rs.getInt("parent_rank_address")));
                    }
                    doc.completePlace(addressPlaces);
                    doc.address(address); // take precedence over computed address
                    doc.setCountry(cnames);

                    var result = NominatimResult.fromAddress(doc, address);

                    if (result.isUsefulForIndex()) {
                        importThread.addDocument(result);
                    }
                });

        final OsmlineRowMapper osmlineRowMapper = new OsmlineRowMapper();
        template.query(
                "SELECT p.place_id, p.osm_id, p.parent_place_id, p.startnumber, p.endnumber, p.postcode, p.country_code, p.linegeo," +
                        (hasNewStyleInterpolation ? " p.step," : " p.interpolationtype,") +
                        "       parent.class as parent_class, parent.type as parent_type," +
                        "       parent.rank_address as parent_rank_address, parent.name as parent_name, " +
                        dbutils.jsonArrayFromSelect(
                                "address_place_id",
                                "FROM place_addressline pa " +
                                        " WHERE pa.place_id IN (p.place_id, coalesce(p.parent_place_id, p.place_id)) AND isaddress" +
                                        " ORDER BY cached_rank_address DESC, pa.place_id = p.place_id DESC") + " as addresslines" +
                        " FROM location_property_osmline p LEFT JOIN placex parent ON p.parent_place_id = parent.place_id" +
                        " WHERE startnumber is not null AND p." + countrySQL +
                        " ORDER BY p.geometry_sector, p.parent_place_id",
                sqlArgs, sqlArgTypes, rs -> {
                    final PhotonDoc doc = osmlineRowMapper.mapRow(rs, 0);

                    final var addressPlaces = addressCache.getAddressList(rs.getString("addresslines"));
                    if (rs.getString("parent_class") != null) {
                        addressPlaces.add(0, new AddressRow(
                                dbutils.getMap(rs, "parent_name"),
                                rs.getString("parent_class"),
                                rs.getString("parent_type"),
                                rs.getInt("parent_rank_address")));
                    }
                    doc.completePlace(addressPlaces);

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
                });

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

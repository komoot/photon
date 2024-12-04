package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.nominatim.model.*;
import org.locationtech.jts.geom.Geometry;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Importer for updates from a Nominatim database.
 */
public class NominatimUpdater extends NominatimConnector {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimUpdater.class);

    private static final String SELECT_COLS_PLACEX = "SELECT place_id, osm_type, osm_id, class, type, name, postcode, address, extratags, ST_Envelope(geometry) AS bbox, parent_place_id, linked_place_id, rank_address, rank_search, importance, country_code, centroid";
    private static final String SELECT_COLS_ADDRESS = "SELECT p.name, p.class, p.type, p.rank_address";
    private static final String SELECT_OSMLINE_OLD_STYLE = "SELECT place_id, osm_id, parent_place_id, startnumber, endnumber, interpolationtype, postcode, country_code, linegeo";
    private static final String SELECT_OSMLINE_NEW_STYLE = "SELECT place_id, osm_id, parent_place_id, startnumber, endnumber, step, postcode, country_code, linegeo";

    private static final String TRIGGER_SQL =
            "DROP TABLE IF EXISTS photon_updates;"
            + "CREATE TABLE photon_updates (rel TEXT, place_id BIGINT,"
            + "                             operation TEXT,"
            + "                             indexed_date TIMESTAMP WITH TIME ZONE);"
            + "CREATE OR REPLACE FUNCTION photon_update_func()\n"
            + " RETURNS TRIGGER AS $$\n"
            + "BEGIN\n"
            + "  INSERT INTO photon_updates("
            + "     VALUES (TG_TABLE_NAME, OLD.place_id, TG_OP, statement_timestamp()));"
            + "  RETURN NEW;"
            + "END; $$ LANGUAGE plpgsql;"
            + "CREATE OR REPLACE TRIGGER photon_trigger_update_placex"
            + "   AFTER UPDATE ON placex FOR EACH ROW"
            + "   WHEN (OLD.indexed_status > 0 AND NEW.indexed_status = 0)"
            + "   EXECUTE FUNCTION photon_update_func();"
            + "CREATE OR REPLACE TRIGGER photon_trigger_delete_placex"
            + "   AFTER DELETE ON placex FOR EACH ROW"
            + "   EXECUTE FUNCTION photon_update_func();"
            + "CREATE OR REPLACE TRIGGER photon_trigger_update_interpolation "
            + "   AFTER UPDATE ON location_property_osmline FOR EACH ROW"
            + "   WHEN (OLD.indexed_status > 0 AND NEW.indexed_status = 0)"
            + "   EXECUTE FUNCTION photon_update_func();"
            + "CREATE OR REPLACE TRIGGER photon_trigger_delete_interpolation"
            + "   AFTER DELETE ON location_property_osmline FOR EACH ROW"
            + "   EXECUTE FUNCTION photon_update_func()";

    private Updater updater;

    /**
     * Map a row from location_property_osmline (address interpolation lines) to a photon doc.
     * This may be old-style interpolation (using interpolationtype) or
     * new-style interpolation (using step).
     */
    private final RowMapper<NominatimResult> osmlineToNominatimResult;


    /**
     * Maps a placex row in nominatim to a photon doc.
     * Some attributes are still missing and can be derived by connected address items.
     */
    private final RowMapper<NominatimResult> placeToNominatimResult;


    /**
     * Lock to prevent thread from updating concurrently.
     */
    private ReentrantLock updateLock = new ReentrantLock();


    // One-item cache for address terms. Speeds up processing of rank 30 objects.
    private long parentPlaceId = -1;
    private List<AddressRow> parentTerms = null;
    private boolean useGeometryColumn;


    public NominatimUpdater(String host, int port, String database, String username, String passwordboolean, boolean useGeometryColumn) {
        this(host, port, database, username, passwordboolean, new PostgisDataAdapter(), useGeometryColumn);
    }

    public NominatimUpdater(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter, boolean useGeometryColumn) {
        super(host, port, database, username, password, dataAdapter, useGeometryColumn);

        final var placeRowMapper = new PlaceRowMapper(dbutils, useGeometryColumn);

        placeToNominatimResult = (rs, rowNum) -> {
            PhotonDoc doc = placeRowMapper.mapRow(rs, rowNum);
            assert (doc != null);

            Map<String, String> address = dbutils.getMap(rs, "address");

            doc.completePlace(getAddresses(doc));
            // Add address last, so it takes precedence.
            doc.address(address);

            doc.setCountry(countryNames.get(rs.getString("country_code")));

            return NominatimResult.fromAddress(doc, address);
        };

        // Setup handling of interpolation table. There are two different formats depending on the Nominatim version.
        // new-style interpolations
        final OsmlineRowMapper osmlineRowMapper = new OsmlineRowMapper();
        osmlineToNominatimResult = (rs, rownum) -> {
            PhotonDoc doc = osmlineRowMapper.mapRow(rs, rownum);

            doc.completePlace(getAddresses(doc));
            doc.setCountry(countryNames.get(rs.getString("country_code")));

            Geometry geometry = dbutils.extractGeometry(rs, "linegeo");

            if (hasNewStyleInterpolation) {
                return NominatimResult.fromInterpolation(
                        doc, rs.getLong("startnumber"), rs.getLong("endnumber"),
                        rs.getLong("step"), geometry);
            }

            return NominatimResult.fromInterpolation(
                    doc, rs.getLong("startnumber"), rs.getLong("endnumber"),
                    rs.getString("interpolationtype"), geometry);
        };
    }



    public boolean isBusy() {
        return updateLock.isLocked();
    }

    public boolean isSetUpForUpdates() {
        Integer result = template.queryForObject("SELECT count(*) FROM pg_tables WHERE tablename = 'photon_updates'", Integer.class);
        return (result != null) && (result > 0);
    }

    public void setUpdater(Updater updater) {
        this.updater = updater;
    }

    public void initUpdates(String updateUser) {
        LOGGER.info("Creating tracking tables");
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                template.execute(TRIGGER_SQL);
                template.execute("GRANT SELECT, DELETE ON photon_updates TO \"" + updateUser + '"');
            }
        });
    }

    public void update() {
        if (updateLock.tryLock()) {
            try {
                loadCountryNames();
                updateFromPlacex();
                updateFromInterpolations();
                updater.finish();
                LOGGER.info("Finished updating");
            } finally {
                updateLock.unlock();
            }
        } else {
            LOGGER.info("Update already in progress");
        }
    }

    private void updateFromPlacex() {
        LOGGER.info("Starting place updates");
        int updatedPlaces = 0;
        int deletedPlaces = 0;
        for (UpdateRow place : getPlaces("placex")) {
            long placeId = place.getPlaceId();
            int objectId = -1;
            boolean checkForMultidoc = true;

            if (!place.isToDelete()) {
                final List<PhotonDoc> updatedDocs = getByPlaceId(placeId);
                if (updatedDocs != null && !updatedDocs.isEmpty() && updatedDocs.get(0).isUsefulForIndex()) {
                    checkForMultidoc = updatedDocs.get(0).getRankAddress() == 30;
                    ++updatedPlaces;
                    for (PhotonDoc updatedDoc : updatedDocs) {
                            updater.create(updatedDoc, ++objectId);
                    }
                }
            }

            if (objectId < 0) {
                ++deletedPlaces;
                updater.delete(placeId, 0);
                objectId = 0;
            }

            if (checkForMultidoc) {
                while (updater.exists(placeId, ++objectId)) {
                    updater.delete(placeId, objectId);
                }
            }
        }

        LOGGER.info("{} places created or updated, {} deleted", updatedPlaces, deletedPlaces);
    }

    /**
     * Update documents generated from address interpolations.
     */
    private void updateFromInterpolations() {
        // .isUsefulForIndex() should always return true for documents
        // created from interpolations so no need to check them
        LOGGER.info("Starting interpolations");
        int updatedInterpolations = 0;
        int deletedInterpolations = 0;
        for (UpdateRow place : getPlaces("location_property_osmline")) {
            long placeId = place.getPlaceId();
            int objectId = -1;

            if (!place.isToDelete()) {
                final List<PhotonDoc> updatedDocs = getInterpolationsByPlaceId(placeId);
                if (updatedDocs != null) {
                    ++updatedInterpolations;
                    for (PhotonDoc updatedDoc : updatedDocs) {
                        updater.create(updatedDoc, ++objectId);
                    }
                }
            }

            if (objectId < 0) {
                ++deletedInterpolations;
            }

            while (updater.exists(placeId, ++objectId)) {
                updater.delete(placeId, objectId);
            }
        }

        LOGGER.info("{} interpolations created or updated, {} deleted", updatedInterpolations, deletedInterpolations);
    }

    private List<UpdateRow> getPlaces(String table) {
        return txTemplate.execute(status -> {
            List<UpdateRow> results = template.query(dbutils.deleteReturning(
                            "DELETE FROM photon_updates WHERE rel = ?", "place_id, operation, indexed_date"),
                    (rs, rowNum) -> {
                        boolean isDelete = "DELETE".equals(rs.getString("operation"));
                        return new UpdateRow(rs.getLong("place_id"), isDelete, rs.getTimestamp("indexed_date"));
                    }, table);

            // For each place only keep the newest item.
            // Order doesn't really matter because updates of each place are independent now.
            results.sort(Comparator.comparing(UpdateRow::getPlaceId).thenComparing(
                    Comparator.comparing(UpdateRow::getUpdateDate).reversed()));

            ArrayList<UpdateRow> todo = new ArrayList<>();
            long prevId = -1;
            for (UpdateRow row : results) {
                if (row.getPlaceId() != prevId) {
                    prevId = row.getPlaceId();
                    todo.add(row);
                }
            }

            return todo;
        });
    }


    public List<PhotonDoc> getByPlaceId(long placeId) {
        String query = SELECT_COLS_PLACEX;

        if (useGeometryColumn) {
            query += ", geometry";
        }

        query += " FROM placex WHERE place_id = ? and indexed_status = 0";

        List<NominatimResult> result = template.query(
                query,
                placeToNominatimResult, placeId);

        return result.isEmpty() ? null : result.get(0).getDocsWithHousenumber();
    }

    public List<PhotonDoc> getInterpolationsByPlaceId(long placeId) {
        List<NominatimResult> result = template.query(
                (hasNewStyleInterpolation ? SELECT_OSMLINE_NEW_STYLE : SELECT_OSMLINE_OLD_STYLE)
                        + " FROM location_property_osmline WHERE place_id = ? and indexed_status = 0",
                osmlineToNominatimResult, placeId);

        return result.isEmpty() ? null : result.get(0).getDocsWithHousenumber();
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
}

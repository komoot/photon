package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.PhotonDocAddressSet;
import de.komoot.photon.PhotonDocInterpolationSet;
import de.komoot.photon.Updater;
import de.komoot.photon.nominatim.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger LOGGER = LogManager.getLogger();

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
     */
    private final RowMapper<Iterable<PhotonDoc>> osmlineToNominatimResult;
    /**
     * Maps a placex row in nominatim to a photon doc.
     * Some attributes are still missing and can be derived by connected address items.
     */
    private final RowMapper<Iterable<PhotonDoc>> placeToNominatimResult;

    /**
     * Lock to prevent thread from updating concurrently.
     */
    private final ReentrantLock updateLock = new ReentrantLock();

    private final String placeBaseSQL;
    private final String osmlineBaseSQL;

    public NominatimUpdater(String host, int port, String database, String username, String password, boolean useGeometryColumn) {
        this(host, port, database, username, password, new PostgisDataAdapter(), useGeometryColumn);
    }

    public NominatimUpdater(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter, boolean useGeometryColumn) {
        super(host, port, database, username, password, dataAdapter, useGeometryColumn);

        final NominatimAddressCache addressCache = new NominatimAddressCache(dataAdapter);

        final var placeRowMapper = new PlaceRowMapper(dbutils, useGeometryColumn);
        placeBaseSQL = placeRowMapper.makeBaseSelect();

        placeToNominatimResult = (rs, rowNum) -> {
            final PhotonDoc doc = placeRowMapper.mapRow(rs, rowNum);
            assert (doc != null);

            if (rs.getInt("rank_search") == 30 && rs.getString("parent_class") != null) {
                doc.completePlace(List.of(new AddressRow(
                        dbutils.getMap(rs, "parent_name"),
                        rs.getString("parent_class"),
                        rs.getString("parent_type"),
                        rs.getInt("parent_rank_address"))));
            }
            doc.completePlace(
                    addressCache.getOrLoadAddressList(template, rs.getString("addresslines")));

            // Add address last, so it takes precedence.
            final var address = dbutils.getMap(rs, "address");
            doc.address(address);

            doc.setCountry(countryNames.get(rs.getString("country_code")));

            return new PhotonDocAddressSet(doc, address);
        };

        // Setup handling of interpolation table.
        final var osmlineRowMapper = new OsmlineRowMapper();
        osmlineBaseSQL = osmlineRowMapper.makeBaseQuery(dataAdapter);
        osmlineToNominatimResult = (rs, rownum) -> {
            PhotonDoc doc = osmlineRowMapper.mapRow(rs, rownum);
            assert doc != null;

            if (rs.getString("parent_class") != null) {
                doc.completePlace(List.of(new AddressRow(
                        dbutils.getMap(rs, "parent_name"),
                        rs.getString("parent_class"),
                        rs.getString("parent_type"),
                        rs.getInt("parent_rank_address"))));
            }
            doc.completePlace(
                    addressCache.getOrLoadAddressList(template, rs.getString("addresslines")));
            doc.address(dbutils.getMap(rs, "address"));
            doc.setCountry(countryNames.get(rs.getString("country_code")));

            Geometry geometry = dbutils.extractGeometry(rs, "linegeo");

            return new PhotonDocInterpolationSet(
                    doc, rs.getLong("startnumber"), rs.getLong("endnumber"),
                    rs.getLong("step"), geometry);
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

            if (place.isToDelete()) {
                ++deletedPlaces;
                updater.delete(placeId);
            } else {
                updater.addOrUpdate(getByPlaceId(placeId));
                ++updatedPlaces;
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

            if (place.isToDelete()) {
                ++deletedInterpolations;
                updater.delete(placeId);
            } else {
                updater.addOrUpdate(getInterpolationsByPlaceId(placeId));
                ++updatedInterpolations;
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

    public Iterable<PhotonDoc> getByPlaceId(long placeId) {
        var result = template.query(
                placeBaseSQL +
                        "     , parent.class as parent_class, parent.type as parent_type," +
                        "       parent.rank_address as parent_rank_address, parent.name as parent_name" +
                        " FROM placex p LEFT JOIN placex parent ON p.parent_place_id = parent.place_id" +
                        " WHERE p.place_id = ? and p.indexed_status = 0",
                placeToNominatimResult, placeId);

        return result.isEmpty() ? List.of() : result.get(0);
    }

    public Iterable<PhotonDoc> getInterpolationsByPlaceId(long placeId) {
        var result = template.query(
                osmlineBaseSQL + " AND p.place_id = ? and p.indexed_status = 0",
                osmlineToNominatimResult, placeId);

        return result.isEmpty() ? List.of() : result.get(0);
    }
}

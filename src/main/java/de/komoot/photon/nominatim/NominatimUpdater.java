package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.nominatim.model.UpdateRow;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Nominatim update logic
 *
 * @author felix
 */

public class NominatimUpdater {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimUpdater.class);

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

    private static final int CREATE = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 100;

    private static final int MIN_RANK = 1;
    private static final int MAX_RANK = 30;

    private final JdbcTemplate       template;
    private final NominatimConnector exporter;

    private Updater updater;

    /**
     * when updating lockout other threads
     */
    private ReentrantLock updateLock = new ReentrantLock();

    public void setUpdater(Updater updater) {
        this.updater = updater;
    }

    public void initUpdates(String updateUser) {
        LOGGER.info("Creating tracking tables");
        template.execute(TRIGGER_SQL);
        template.execute("GRANT SELECT, DELETE ON photon_updates TO \"" + updateUser + '"');
    }

    public void update() {
        if (updateLock.tryLock()) {
            try {
                update_from_placex();
                update_from_interpolations();
                updater.finish();
                LOGGER.info("Finished updating");
            } finally {
                updateLock.unlock();
            }
        } else {
            LOGGER.info("Update already in progress");
        }
    }

    private void update_from_placex() {
        LOGGER.info("Starting place updates");
        int updatedPlaces = 0;
        int deletedPlaces = 0;
        for (UpdateRow place : getPlaces("placex")) {
            long placeId = place.getPlaceId();
            int object_id = -1;
            boolean check_for_multidoc = true;

            if (!place.isToDelete()) {
                final List<PhotonDoc> updatedDocs = exporter.getByPlaceId(placeId);
                if (updatedDocs != null && !updatedDocs.isEmpty() && updatedDocs.get(0).isUsefulForIndex()) {
                    check_for_multidoc = updatedDocs.get(0).getRankAddress() == 30;
                    ++updatedPlaces;
                    for (PhotonDoc updatedDoc : updatedDocs) {
                            updater.create(updatedDoc, ++object_id);
                    }
                }
            }

            if (object_id < 0) {
                ++deletedPlaces;
                updater.delete(placeId, 0);
                object_id = 0;
            }

            if (check_for_multidoc) {
                while (updater.exists(placeId, ++object_id)) {
                    updater.delete(placeId, object_id);
                }
            }
        }

        LOGGER.info("{} places created or updated, {} deleted", updatedPlaces, deletedPlaces);
    }

    /**
     * Update documents generated from address interpolations.
     */
    private void update_from_interpolations() {
        // .isUsefulForIndex() should always return true for documents
        // created from interpolations so no need to check them
        LOGGER.info("Starting interpolations");
        int updatedInterpolations = 0;
        int deletedInterpolations = 0;
        for (UpdateRow place : getPlaces("location_property_osmline")) {
            long placeId = place.getPlaceId();
            int object_id = -1;

            if (!place.isToDelete()) {
                final List<PhotonDoc> updatedDocs = exporter.getInterpolationsByPlaceId(placeId);
                if (updatedDocs != null) {
                    ++updatedInterpolations;
                    for (PhotonDoc updatedDoc : updatedDocs) {
                        updater.create(updatedDoc, ++object_id);
                    }
                }
            }

            if (object_id < 0) {
                ++deletedInterpolations;
            }

            while (updater.exists(placeId, ++object_id)) {
                updater.delete(placeId, object_id);
            }
        }

        LOGGER.info("{} interpolations created or updated, {} deleted", updatedInterpolations, deletedInterpolations);
    }

    private List<UpdateRow> getPlaces(String table) {
        List<UpdateRow> results = template.query(exporter.getDataAdaptor().deleteReturning(
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
        long prev_id = -1;
        for (UpdateRow row: results) {
            if (row.getPlaceId() != prev_id) {
                prev_id = row.getPlaceId();
                todo.add(row);
            }
        }

        return todo;
    }


    /**
     * Creates a new instance
     * 
     * @param host Nominatim database host
     * @param port Nominatim database port
     * @param database Nominatim database name
     * @param username Nominatim database username
     * @param password Nominatim database password
     */
    public NominatimUpdater(String host, int port, String database, String username, String password, DBDataAdapter dataAdapter) {
        BasicDataSource dataSource = NominatimConnector.buildDataSource(host, port, database, username, password, true);

        exporter = new NominatimConnector(host, port, database, username, password, dataAdapter);
        template = new JdbcTemplate(dataSource);
    }

    public NominatimUpdater(String host, int port, String database, String username, String password) {
        this(host, port, database, username, password, new PostgisDataAdapter());
    }
}

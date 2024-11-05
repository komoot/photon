package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.nominatim.model.UpdateRow;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Importer for updates from a Nominatim database.
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

    private final JdbcTemplate       template;
    private final NominatimConnector exporter;

    private Updater updater;

    /**
     * Lock to prevent thread from updating concurrently.
     */
    private ReentrantLock updateLock = new ReentrantLock();

    public Date getLastImportDate() {
        return exporter.getLastImportDate();
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
        template.execute(TRIGGER_SQL);
        template.execute("GRANT SELECT, DELETE ON photon_updates TO \"" + updateUser + '"');
    }

    public void update() {
        if (updateLock.tryLock()) {
            try {
                exporter.loadCountryNames();
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
                final List<PhotonDoc> updatedDocs = exporter.getByPlaceId(placeId);
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
                final List<PhotonDoc> updatedDocs = exporter.getInterpolationsByPlaceId(placeId);
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
        long prevId = -1;
        for (UpdateRow row: results) {
            if (row.getPlaceId() != prevId) {
                prevId = row.getPlaceId();
                todo.add(row);
            }
        }

        return todo;
    }


    /**
     * Create a new instance.
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

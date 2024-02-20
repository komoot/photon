package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.nominatim.model.UpdateRow;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

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

            // Always delete to catch some corner cases around places with exploded housenumbers.
            updater.delete(placeId);

            if (place.isToDelete()) {
                deletedPlaces++;
                continue;
            }

            final List<PhotonDoc> updatedDocs = exporter.getByPlaceId(placeId);
            if (updatedDocs != null) {
                updatedPlaces++;
                for (PhotonDoc updatedDoc : updatedDocs) {
                    if (updatedDoc.isUsefulForIndex()) {
                        updater.create(updatedDoc);
                    }
                }
            }
        }

        LOGGER.info(String.format("%d places created or updated, %d deleted", updatedPlaces, deletedPlaces));
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
        int interpolationDocuments = 0;
        for (UpdateRow place : getPlaces("location_property_osmline")) {
            long placeId = place.getPlaceId();

            updater.delete(placeId);
            if (place.isToDelete()) {
                deletedInterpolations++;
                continue;
            }

            final List<PhotonDoc> updatedDocs = exporter.getInterpolationsByPlaceId(place.getPlaceId());
            if (updatedDocs != null) {
                updatedInterpolations++;
                for (PhotonDoc updatedDoc : updatedDocs) {
                    updater.create(updatedDoc);
                    interpolationDocuments++;
                }
            }
        }
        LOGGER.info(String.format("%d interpolations created or updated, %d deleted, %d documents added or updated", updatedInterpolations,
                deletedInterpolations, interpolationDocuments));

    }

    private List<UpdateRow> getPlaces(String table) {
        List<UpdateRow> results = template.query("DELETE FROM photon_updates WHERE rel = ? RETURNING place_id, operation, indexed_date",
                (rs, rowNum) -> {
                    boolean isDelete = "DELETE".equals(rs.getString("operation"));
                    return new UpdateRow(rs.getLong("place_id"), isDelete, rs.getDate("indexed_date"));
                }, new Object[]{table});

        results.sort(Comparator.comparing(UpdateRow::getUpdateDate));

        return results;
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
    public NominatimUpdater(String host, int port, String database, String username, String password) {
        BasicDataSource dataSource = NominatimConnector.buildDataSource(host, port, database, username, password, true);

        exporter = new NominatimConnector(host, port, database, username, password);
        template = new JdbcTemplate(dataSource);
    }
}

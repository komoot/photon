package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.nominatim.model.UpdateRow;
import org.apache.commons.dbcp2.BasicDataSource;
import org.postgis.jts.JtsWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Nominatim update logic
 *
 * @author felix
 */

public class NominatimUpdater {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimUpdater.class);

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

    public void update() {
        if (updateLock.tryLock()) {
            try {
                int updatedPlaces = 0;
                int deletedPlaces = 0;
                for (int rank = MIN_RANK; rank <= MAX_RANK; rank++) {
                    LOGGER.info(String.format("Starting rank %d", rank));
                    for (Map<String, Object> sector : getIndexSectors(rank)) {
                        for (UpdateRow place : getIndexSectorPlaces(rank, (Integer) sector.get("geometry_sector"))) {
                            long placeId = place.getPlaceId();
                            template.update("update placex set indexed_status = 0 where place_id = ?;", placeId);

                            Integer indexedStatus = place.getIndexdStatus();
                            if (indexedStatus == DELETE || (indexedStatus == UPDATE && rank == MAX_RANK)) {
                                updater.delete(placeId);
                                if (indexedStatus == DELETE) {
                                    deletedPlaces++;
                                    continue;
                                }
                                indexedStatus = CREATE; // always create
                            }
                            updatedPlaces++;

                            final List<PhotonDoc> updatedDocs = exporter.getByPlaceId(place.getPlaceId());
                            boolean wasUseful = false;
                            for (PhotonDoc updatedDoc : updatedDocs) {
                                switch (indexedStatus) {
                                case CREATE:
                                    if (updatedDoc.isUsefulForIndex()) {
                                        updater.create(updatedDoc);
                                    }
                                    break;
                                case UPDATE:
                                    if (updatedDoc.isUsefulForIndex()) {
                                        updater.updateOrCreate(updatedDoc);
                                        wasUseful = true;
                                    }
                                    break;
                                default:
                                    LOGGER.error(String.format("Unknown index status %d", indexedStatus));
                                    break;
                                }
                            }
                            if (indexedStatus == UPDATE && !wasUseful) {
                                // only true when rank != 30
                                // if no documents for the place id exist this will likely cause moaning
                                updater.delete(placeId);
                                updatedPlaces--;
                            }
                        }
                    }
                }

                LOGGER.info(String.format("%d places created or updated, %d deleted", updatedPlaces, deletedPlaces));

                // update documents generated from address interpolations
                // .isUsefulForIndex() should always return true for documents
                // created from interpolations so no need to check them
                LOGGER.info("Starting interpolations");
                int updatedInterpolations = 0;
                int deletedInterpolations = 0;
                int interpolationDocuments = 0;
                for (Map<String, Object> sector : template.queryForList(
                        "select geometry_sector,count(*) from location_property_osmline where indexed_status > 0 group by geometry_sector order by geometry_sector;")) {
                    for (UpdateRow place : getIndexSectorInterpolations((Integer) sector.get("geometry_sector"))) {
                        long placeId = place.getPlaceId();
                        template.update("update location_property_osmline set indexed_status = 0 where place_id = ?;", placeId);

                        Integer indexedStatus = place.getIndexdStatus();
                        if (indexedStatus != CREATE) {
                            updater.delete(placeId);
                            if (indexedStatus == DELETE) {
                                deletedInterpolations++;
                                continue;
                            }
                        }
                        updatedInterpolations++;

                        final List<PhotonDoc> updatedDocs = exporter.getInterpolationsByPlaceId(place.getPlaceId());
                        for (PhotonDoc updatedDoc : updatedDocs) {
                            updater.create(updatedDoc);
                            interpolationDocuments++;
                        }
                    }
                }
                LOGGER.info(String.format("%d interpolations created or updated, %d deleted, %d documents added or updated", updatedInterpolations,
                        deletedInterpolations, interpolationDocuments));
                updater.finish();
                template.update("update import_status set indexed=true;"); // indicate that we are finished

                LOGGER.info("Finished updating");
            } finally {
                updateLock.unlock();
            }
        } else {
            LOGGER.info("Update already in progress");
        }
    }

    private List<Map<String, Object>> getIndexSectors(Integer rank) {
        return template.queryForList("select geometry_sector,count(*) from placex where rank_search = ? "
                + "and indexed_status > 0 group by geometry_sector order by geometry_sector;", rank);
    }

    private List<UpdateRow> getIndexSectorPlaces(Integer rank, Integer geometrySector) {
        return template.query("select place_id, indexed_status from placex where rank_search = ?" + " and geometry_sector = ? and indexed_status > 0;",
                new Object[] { rank, geometrySector }, new RowMapper<UpdateRow>() {
                    @Override
                    public UpdateRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                        UpdateRow updateRow = new UpdateRow();
                        updateRow.setPlaceId(rs.getLong("place_id"));
                        updateRow.setIndexdStatus(rs.getInt("indexed_status"));
                        return updateRow;
                    }
                });
    }

    private List<UpdateRow> getIndexSectorInterpolations(Integer geometrySector) {
        return template.query("select place_id, indexed_status from location_property_osmline where geometry_sector = ? and indexed_status > 0;",
                new Object[] { geometrySector }, new RowMapper<UpdateRow>() {
                    @Override
                    public UpdateRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                        UpdateRow updateRow = new UpdateRow();
                        updateRow.setPlaceId(rs.getLong("place_id"));
                        updateRow.setIndexdStatus(rs.getInt("indexed_status"));
                        return updateRow;
                    }
                });
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
        BasicDataSource dataSource = NominatimConnector.buildDataSource(host, port, database, username, password);

        exporter = new NominatimConnector(host, port, database, username, password);
        template = new JdbcTemplate(dataSource);
    }
}

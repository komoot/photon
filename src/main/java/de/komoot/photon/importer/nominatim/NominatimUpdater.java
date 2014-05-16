package de.komoot.photon.importer.nominatim;

import de.komoot.photon.importer.nominatim.model.UpdateRow;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.postgis.jts.JtsWrapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.sql.ResultSet;

import de.komoot.photon.importer.Updater;

/***
 * Nominatim update logic
 *
 * @author felix
 */

public class NominatimUpdater {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimUpdater.class);
    private Integer minRank = 1;
    private Integer maxRank = 30;
    private final JdbcTemplate template;
    private NominatimSource exporter;

    private Updater updater;


    public void update(){
        for(Integer rank = this.minRank; rank <= this.maxRank; rank++){
            LOGGER.info(String.format("Starting rank %d", rank));
            for(Map<String, Object> sector: getIndexSectors(rank))
            {
                 for(UpdateRow place: getIndexSectorPlaces(rank, (Integer)sector.get("geometry_sector"))){

                     place.getIndexStatus();
                     template.update("update placex set indexed_status = 0 where place_id = ?", new Object[]{place.getPlaceId()});

                     switch (place.getIndexStatus()){
                         case 1:
                             updater.create(exporter.getByPlaceId(place.getPlaceId()));

                         case 2:
                             updater.update(exporter.getByPlaceId(place.getPlaceId()));

                         case 100:
                            updater.delete(place.getPlaceId());

                         default:
                             LOGGER.error(String.format("Unknown index status %d", place.getIndexStatus()));

                    }

                 }
            }
        }

        updater.finish();
    }


    private List<Map<String, Object>> getIndexSectors(Integer rank){

        return template.queryForList("select geometry_sector,count(*) from placex where rank_search = ? " +
                "and indexed_status > 0 group by geometry_sector order by geometry_sector;", new Object[]{rank});
    }


    private List<UpdateRow> getIndexSectorPlaces(Integer rank, Integer geometrySector){
       return template.query("select place_id, index_status from placex where rank_search = ?" +
               " and geometry_sector = ? and indexed_status > 0;", new Object[]{rank, geometrySector}, new RowMapper<UpdateRow>() {
           @Override
           public UpdateRow mapRow(ResultSet rs, int rowNum) throws SQLException {
               UpdateRow updateRow = new UpdateRow();
               updateRow.setPlaceId(rs.getLong("place_id"));
               updateRow.setIndexStatus(rs.getInt("index_status"));
               return updateRow;
           }});
    }


    /**
     * @param host     database host
     * @param port     database port
     * @param database database name
     * @param username db username
     * @param password db username's password
     */
    public NominatimUpdater(String host, int port, String database, String username, String password) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(JtsWrapper.class.getCanonicalName());
        dataSource.setDefaultAutoCommit(false);

        exporter = new NominatimSource(host, port, database, username, password);

        template = new JdbcTemplate(dataSource);
    }

}

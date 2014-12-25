package de.komoot.photon.importer.nominatim;

import de.komoot.photon.importer.Updater;
import de.komoot.photon.importer.model.PhotonDoc;
import de.komoot.photon.importer.nominatim.model.UpdateRow;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.dbcp.BasicDataSource;
import org.postgis.jts.JtsWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Nominatim update logic
 *
 * @author felix
 */

public class NominatimUpdater {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimUpdater.class);
	private Integer minRank = 1;
	private Integer maxRank = 30;
	private final JdbcTemplate template;
	private NominatimConnector exporter;
        private final String tagWhitelistPath;
        private JSONObject tagWhitelistObject;

	private Updater updater;

	public void setUpdater(Updater updater) {
		this.updater = updater;
	}

	public void update() {
                if(tagWhitelistPath != null) {
                        LOGGER.info("reading tag whitelist from specified file: "+ tagWhitelistPath);
                        String tagWhitelistString = null;
                        try {
                                FileInputStream tagWhitelistStream = new FileInputStream(new File(tagWhitelistPath));
                                tagWhitelistString = IOUtils.toString(tagWhitelistStream, "UTF-8");                            
                        } catch (IOException e) {
                                LOGGER.error("cannot read tag whitelist file", e);
                        }
                        try {
                                JSONObject whitelist = new JSONObject(tagWhitelistString);
                                tagWhitelistObject = whitelist.getJSONObject("tags");                            
                        } catch (JSONException e) {
                                LOGGER.error("cannot parse tag whitelist file", e);
                        }                        
                }
		for(Integer rank = this.minRank; rank <= this.maxRank; rank++) {
			LOGGER.info(String.format("Starting rank %d", rank));
			for(Map<String, Object> sector : getIndexSectors(rank))
				for(UpdateRow place : getIndexSectorPlaces(rank, (Integer) sector.get("geometry_sector"))) {

					template.update("update placex set indexed_status = 0 where place_id = ?", new Object[]{place.getPlaceId()});
					final PhotonDoc updatedDoc = exporter.getByPlaceId(place.getPlaceId());

					switch(place.getIndexdStatus()) {
						case 1:
							if(updatedDoc.isUsefulForIndex(tagWhitelistObject))
								updater.create(updatedDoc);
							break;
						case 2:
							if(!updatedDoc.isUsefulForIndex(tagWhitelistObject))
								updater.delete(place.getPlaceId());

							updater.updateOrCreate(updatedDoc);
							break;
						case 100:
							updater.delete(place.getPlaceId());
							break;
						default:
							LOGGER.error(String.format("Unknown index status %d", place.getIndexdStatus()));
							break;
					}
				}
		}

		updater.finish();
	}

	private List<Map<String, Object>> getIndexSectors(Integer rank) {
		return template.queryForList("select geometry_sector,count(*) from placex where rank_search = ? " +
				"and indexed_status > 0 group by geometry_sector order by geometry_sector;", new Object[]{rank});
	}

	private List<UpdateRow> getIndexSectorPlaces(Integer rank, Integer geometrySector) {
		return template.query("select place_id, indexed_status from placex where rank_search = ?" +
				" and geometry_sector = ? and indexed_status > 0;", new Object[]{rank, geometrySector}, new RowMapper<UpdateRow>() {
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
	 */
	public NominatimUpdater(String host, int port, String database, String username, String password, String tagWhitelistPath) {
		BasicDataSource dataSource = new BasicDataSource();

		dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setDriverClassName(JtsWrapper.class.getCanonicalName());
		dataSource.setDefaultAutoCommit(false);

		exporter = new NominatimConnector(host, port, database, username, password, tagWhitelistPath);
		template = new JdbcTemplate(dataSource);
                this.tagWhitelistPath = tagWhitelistPath;
                this.tagWhitelistObject = null;
	}
}

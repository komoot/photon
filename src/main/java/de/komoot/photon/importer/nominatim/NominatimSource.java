package de.komoot.photon.importer.nominatim;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.importer.Importer;
import de.komoot.photon.importer.model.PhotonDoc;
import lombok.extern.log4j.Log4j;
import org.apache.commons.dbcp.BasicDataSource;
import org.postgis.jts.JtsWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Export nominatim data
 *
 * @author felix, christoph
 */
@Log4j
public class NominatimSource {
	private Connection pgConnection;
	private int fetchSize = 10000;
	private final JdbcTemplate template;

    private Importer importer;

    public void setImporter(Importer importer){
        this.importer = importer;
    }

	private final RowMapper<PhotonDoc> placeRowMapper = new RowMapper<PhotonDoc>() {
		@Override
		public PhotonDoc mapRow(ResultSet rs, int rowNum) throws SQLException {

			Double importance = rs.getDouble("importance");
			if(importance == null) {
				// https://github.com/komoot/photon/issues/12
				int rankSearch = rs.getInt("rank_search");
				importance = 0.75 - rankSearch / 40d;
			}

			Geometry geometry = Utils.extractGeometry(rs, "bbox");
			Envelope envelope = geometry != null ? geometry.getEnvelopeInternal() : null;

			PhotonDoc doc = new PhotonDoc(
					rs.getLong("place_id"),
					rs.getString("osm_type"),
					rs.getLong("osm_id"),
					rs.getString("class"),
					rs.getString("type"),
					Utils.getMap(rs, "name"),
					rs.getString("housenumber"),
					Utils.getMap(rs, "extratags"),
					envelope,
					rs.getLong("parent_place_id"),
					importance,
					CountryCode.getByCode(rs.getString("calculated_country_code")),
					(Point) Utils.extractGeometry(rs, "centroid"),
					rs.getLong("linked_place_id")
			);

			doc.setPostcode(rs.getString("postcode"));

			return doc;
		}
	};
	private final String selectColumns = "place_id, osm_type, osm_id, class, type, name, housenumber, postcode, extratags, ST_Envelope(geometry) AS bbox, parent_place_id, linked_place_id, rank_search, importance, calculated_country_code, centroid";

	public PhotonDoc getByPlaceId(long placeId) {
		return template.queryForObject("SELECT " + selectColumns + " FROM placex WHERE place_id = ?", new Object[]{placeId}, placeRowMapper);
	}

	public void export() {
		template.query("SELECT " + selectColumns + " FROM placex WHERE linked_place_id IS NULL LIMIT 10; ", new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				PhotonDoc doc = placeRowMapper.mapRow(rs, 0);

                importer.addDocument(doc);


			}
		});

        importer.finish();

		//		long counter = 0;
		//		long time = System.currentTimeMillis();
		//
		//		try {
		//			Statement query = this.pgConnection.createStatement();
		//			query.setFetchSize(this.fetchSize);
		//
		//			ResultSet results = query.executeQuery("SELECT placex.place_id as place_id, name, housenumber, st_y(centroid) as lat, st_x(centroid) as lon, admin_level from placex limit 20;");
		//			while(results.next()) {
		//				if(counter % 10000 == 0 && counter > 0) {
		//					LOGGER.info(String.format("progress: %10d entries [%.1f / second]", counter, 10000000. / (1. * System.currentTimeMillis() - time)));
		//					time = System.currentTimeMillis();
		//				}
		//
		//				Statement detailQuery = this.pgConnection.createStatement();
		//				ResultSet detailResults = detailQuery.executeQuery("SELECT place_id, osm_type, osm_id, name->'name' as name, name->'ref' as name_ref, name->'place_name' as place_name, name->'short_name' as short_name, name->'official_name' as official_name, class," +
		//						" type, admin_level, rank_address FROM get_addressdata(" + results.getString("place_id") + ") WHERE isaddress ORDER BY rank_address DESC");
		//				while(detailResults.next()) {
		//					LOGGER.info(String.format("%s - %s - %s", detailResults.getString("name"), detailResults.getString("admin_level"), detailResults.getString("rank_address")));
		//				}
		//
		//				LOGGER.info(results.getString(1));
		//			}
		//		} catch(SQLException e) {
		//			e.printStackTrace();
		//		}
	}

	/**
	 * @param host     database host
	 * @param port     database port
	 * @param database database name
	 * @param username db username
	 * @param password db username's password
	 */
	public NominatimSource(String host, int port, String database, String username, String password) {
		BasicDataSource dataSource = new BasicDataSource();

		dataSource.setUrl(String.format("jdbc:postgres_jts://%s:%d/%s", host, port, database));
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setDriverClassName(JtsWrapper.class.getCanonicalName());
		dataSource.setDefaultAutoCommit(false);

		template = new JdbcTemplate(dataSource);
		template.setFetchSize(100000);
	}
}

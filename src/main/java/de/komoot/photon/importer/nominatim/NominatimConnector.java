package de.komoot.photon.importer.nominatim;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import de.komoot.photon.importer.Importer;
import de.komoot.photon.importer.model.PhotonDoc;
import de.komoot.photon.importer.nominatim.model.AddressRow;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.dbcp.BasicDataSource;
import org.postgis.jts.JtsWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Export nominatim data
 *
 * @author felix, christoph
 */
@Slf4j
public class NominatimConnector {
	private final JdbcTemplate template;
	/**
	 * maps a placex row in nominatim to a photon doc, some attributes are still missing and can be derived by connected address items.
	 */
	private final RowMapper<PhotonDoc> placeRowMapper = new RowMapper<PhotonDoc>() {
		@Override
		public PhotonDoc mapRow(ResultSet rs, int rowNum) throws SQLException {

			Double importance = rs.getDouble("importance");
			if(rs.wasNull()) {
				// https://github.com/komoot/photon/issues/12
				int rankSearch = rs.getInt("rank_search");
				importance = 0.75 - rankSearch / 40d;
			}

			Geometry geometry = DBUtils.extractGeometry(rs, "bbox");
			Envelope envelope = geometry != null ? geometry.getEnvelopeInternal() : null;

			PhotonDoc doc = new PhotonDoc(
					rs.getLong("place_id"),
					rs.getString("osm_type"),
					rs.getLong("osm_id"),
					rs.getString("class"),
					rs.getString("type"),
					DBUtils.getMap(rs, "name"),
					rs.getString("housenumber"),
					DBUtils.getMap(rs, "extratags"),
					envelope,
					rs.getLong("parent_place_id"),
					importance,
					CountryCode.getByCode(rs.getString("calculated_country_code")),
					(Point) DBUtils.extractGeometry(rs, "centroid"),
					rs.getLong("linked_place_id")
			);
			doc.setPostcode(rs.getString("postcode"));
			return doc;
		}
	};
	private final String selectColsPlaceX = "place_id, osm_type, osm_id, class, type, name, housenumber, postcode, extratags, ST_Envelope(geometry) AS bbox, parent_place_id, linked_place_id, rank_search, importance, calculated_country_code, centroid";
	private Importer importer;

	/**
	 * @param host     database host
	 * @param port     database port
	 * @param database database name
	 * @param username db username
	 * @param password db username's password
	 */
	public NominatimConnector(String host, int port, String database, String username, String password) {
		BasicDataSource dataSource = new BasicDataSource();

		dataSource.setUrl(String.format("jdbc:postgres_jts://%s:%d/%s", host, port, database));
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setDriverClassName(JtsWrapper.class.getCanonicalName());
		dataSource.setDefaultAutoCommit(false);

		template = new JdbcTemplate(dataSource);
		template.setFetchSize(100000);
	}

	public void setImporter(Importer importer) {
		this.importer = importer;
	}

	public PhotonDoc getByPlaceId(long placeId) {
		return template.queryForObject("SELECT " + selectColsPlaceX + " FROM placex WHERE place_id = ?", new Object[]{placeId}, placeRowMapper);
	}

	public List<AddressRow> getAddresses(long placeId) {
		return template.query("SELECT place_id, name, class, type, rank_address, admin_level FROM get_addressdata(?) WHERE isaddress AND (place_id IS NULL OR place_id != ?)", new Object[]{placeId, placeId}, new RowMapper<AddressRow>() {
			@Override
			public AddressRow mapRow(ResultSet rs, int rowNum) throws SQLException {
				Integer adminLevel = rs.getInt("admin_level");
				if(rs.wasNull()) {
					adminLevel = null;
				}
				return new AddressRow(
						rs.getLong("place_id"),
						DBUtils.getMap(rs, "name"),
						rs.getString("class"),
						rs.getString("type"),
						rs.getInt("rank_address"),
						adminLevel
				);
			}
		});
	}
	
	
	private class ImportThread implements Runnable {
		private BlockingQueue<PhotonDoc> documents;
		
		public ImportThread(BlockingQueue<PhotonDoc> documents) {
			this.documents = documents;
		}
		

		@Override
		public void run() {
			while (true) {
				PhotonDoc doc = null;
				try {
					doc = documents.take();
					if (doc == null)
						break;
					importer.add(doc);
				} catch (InterruptedException e) { /* safe to ignore? */ }
				
			}
			importer.finish();
		}
				
	}

	/**
	 * parses every relevant row in placex, creates a corresponding document and calls the {@link #importer} for every document
	 */
	public void readEntireDatabase() {
		log.info("start importing documents from nominatim ...");
		final AtomicLong counter = new AtomicLong();

		final int progressInterval = 5000;
		final long startMillis = System.currentTimeMillis();
		
		final BlockingQueue<PhotonDoc> documents = new LinkedBlockingDeque<PhotonDoc>(20);
		Thread importThread = new Thread(new ImportThread(documents));
		importThread.start();

		template.query("SELECT " + selectColsPlaceX + " FROM placex WHERE linked_place_id IS NULL ORDER BY parent_place_id;", new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				PhotonDoc doc = placeRowMapper.mapRow(rs, 0);

				// finalize document by taking into account the higher level address assigned to this doc.
				final List<AddressRow> addresses = getAddresses(doc.getPlaceId());
				for(AddressRow address : addresses) {
					if(address.isCity()) {
						if(doc.getCity() != null) {
							doc.getContext().add(doc.getCity());
						}
						doc.setCity(address.getName());
					} else if(address.isStreet() && doc.getStreet() == null) {
						doc.setStreet(address.getName());
					} else if(address.isPostcode() && doc.getPostcode() == null && address.getName() != null) {
						doc.setPostcode(address.getName().get("ref"));
					} else if(address.isCountry()) {
						doc.setCountry(address.getName());
					} else {
						if(address.isUsefulForContext()) {
							doc.getContext().add(address.getName());
						}
					}
				}

				if(!doc.isUsefulForIndex()) return; // do not import document

				//importer.add(doc);
				while (true) {
					try {
						documents.put(doc);
					} catch (InterruptedException e) {
						log.warn("Thread interrupted while placing document in queue.");
						continue;
					}
					break;
				}
				if(counter.incrementAndGet() % progressInterval == 0) {
					final double documentsPerSecond = 1000d * counter.longValue() / (System.currentTimeMillis() - startMillis);
					log.info(String.format("imported %s documents [%.1f/second]", MessageFormat.format("{0}", counter.longValue()), documentsPerSecond));
				}
			}
		});

		while (true) {
			try {
				documents.put(null);
				importThread.join();
			} catch (InterruptedException e) {
				log.warn("Thread interrupted while placing document in queue.");
				continue;
			}
			break;
		}
		//importer.finish();
		log.info(String.format("finished import of %s photon documents.", MessageFormat.format("{0}", counter.longValue())));
	}
}

package de.komoot.photon.nominatim;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;
import org.postgis.jts.JtsWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private Map<String, Map<String, String>> countryNames;
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
					rs.getLong("linked_place_id"),
					rs.getInt("rank_search")
			);
			doc.setPostcode(rs.getString("postcode"));
			doc.setCountry(getCountryNames(rs.getString("calculated_country_code")));
			return doc;
		}
	};
	private final String selectColsPlaceX = "place_id, osm_type, osm_id, class, type, name, housenumber, postcode, extratags, ST_Envelope(geometry) AS bbox, parent_place_id, linked_place_id, rank_search, importance, calculated_country_code, centroid";
	private Importer importer;

	private Map<String, String> getCountryNames(String countrycode) {
		if(countryNames == null) {
			countryNames = new HashMap<String, Map<String, String>>();
			template.query("SELECT country_code, name FROM country_name;", new RowCallbackHandler() {
						@Override
						public void processRow(ResultSet rs) throws SQLException {
							countryNames.put(rs.getString("country_code"), DBUtils.getMap(rs, "name"));
						}
					}
			);
		}

		return countryNames.get(countrycode);
	}

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

	List<AddressRow> getAddresses(PhotonDoc doc) {
		long placeId = doc.getPlaceId();
		if(doc.getRankSearch() > 28)
			placeId = doc.getParentPlaceId();
		return template.query("SELECT p.place_id, p.osm_type, p.osm_id, p.name, p.class, p.type, p.rank_address, p.admin_level, p.postcode, p.extratags->'place' as place FROM placex p, place_addressline pa WHERE p.place_id = pa.address_place_id and pa.place_id = ? and pa.cached_rank_address > 4 and pa.address_place_id != ? and pa.isaddress order by rank_address desc,fromarea desc,distance asc,rank_search desc", new Object[]{placeId, doc.getPlaceId()}, new RowMapper<AddressRow>() {
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
						adminLevel,
						rs.getString("postcode"),
						rs.getString("place"),
						rs.getString("osm_type"),
						rs.getLong("osm_id")
				);
			}
		});
	}

	private static final PhotonDoc FINAL_DOCUMENT = new PhotonDoc(0, null, 0, null, null, null, null, null, null, 0, 0, null, null, 0, 0);

	private class ImportThread implements Runnable {
		private final BlockingQueue<PhotonDoc> documents;

		public ImportThread(BlockingQueue<PhotonDoc> documents) {
			this.documents = documents;
		}

		@Override
		public void run() {
			while(true) {
				PhotonDoc doc;
				try {
					doc = documents.take();
					if(doc == FINAL_DOCUMENT)
						break;
					importer.add(doc);
				} catch(InterruptedException e) { /* safe to ignore? */ }
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

		final int progressInterval = 50000;
		final long startMillis = System.currentTimeMillis();

		final BlockingQueue<PhotonDoc> documents = new LinkedBlockingDeque<PhotonDoc>(20);
		Thread importThread = new Thread(new ImportThread(documents));
		importThread.start();

		template.query("SELECT " + selectColsPlaceX + " FROM placex WHERE linked_place_id IS NULL order by geometry_sector; ", new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				// turns a placex row into a photon document that gathers all de-normalised information

				PhotonDoc doc = placeRowMapper.mapRow(rs, 0);

				if(!doc.isUsefulForIndex()) return; // do not import document

				// finalize document by taking into account the higher level placex rows assigned to this row
				completePlace(doc);

				//importer.add(doc);
				while(true) {
					try {
						documents.put(doc);
					} catch(InterruptedException e) {
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

		while(true) {
			try {
				documents.put(FINAL_DOCUMENT);
				importThread.join();
			} catch(InterruptedException e) {
				log.warn("Thread interrupted while placing document in queue.");
				continue;
			}
			break;
		}
		//importer.finish();
		log.info(String.format("finished import of %s photon documents.", MessageFormat.format("{0}", counter.longValue())));
	}

	/**
	 * retrieves a single document, used for testing / developing
	 *
	 * @param osmId
	 * @param osmType 'N': node, 'W': way or 'R' relation
	 * @return
	 */
	public List<PhotonDoc> readDocument(long osmId, char osmType) {
		return template.query("SELECT " + selectColsPlaceX + " FROM placex WHERE osm_id = ? AND osm_type = ?; ", new Object[]{osmId, osmType}, new RowMapper<PhotonDoc>() {
			@Override
			public PhotonDoc mapRow(ResultSet resultSet, int i) throws SQLException {
				PhotonDoc doc = placeRowMapper.mapRow(resultSet, 0);
				completePlace(doc);
				return doc;
			}
		});
	}

	/**
	 * querying nominatim's address hierarchy to complete photon doc with missing data (like country, city, street, ...)
	 *
	 * @param doc
	 */
	private void completePlace(PhotonDoc doc) {
		final List<AddressRow> addresses = getAddresses(doc);
		for(AddressRow address : addresses) {

			if(address.hasPostcode() && doc.getPostcode() == null) {
				doc.setPostcode(address.getPostcode());
			}

			if(address.isCity()) {
				if(doc.getCity() == null) {
					doc.setCity(address.getName());
				} else {
					// there is more than one city address for this document
					if(address.hasPlace()) {
						// this city is more important than the previous one
						doc.getContext().add(doc.getCity()); // move previous city to context
						doc.setCity(address.getName()); // use new city
					} else {
						doc.getContext().add(address.getName());
					}
				}
				continue;
			}

			if(address.isCuratedCity()) {
				if(doc.getCity() == null) {
					doc.setCity(address.getName());
				} else {
					doc.getContext().add(doc.getCity()); // move previous city to context
					doc.setCity(address.getName()); // use new city
				}
				// do not continue as a curated city might be a state as well
			}

			if(address.isStreet() && doc.getStreet() == null) {
				doc.setStreet(address.getName());
				continue;
			}

			if(address.isState() && doc.getState() == null) {
				doc.setState(address.getName());
				continue;
			}

			// no specifically handled item, check if useful for context
			if(address.isUsefulForContext()) {
				doc.getContext().add(address.getName());
			}
		}
	}
}

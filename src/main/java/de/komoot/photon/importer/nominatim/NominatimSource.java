package de.komoot.photon.importer.nominatim;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.importer.Importer;
import de.komoot.photon.importer.model.PhotonDoc;
import de.komoot.photon.importer.nominatim.model.AddressRow;
import org.apache.commons.dbcp.BasicDataSource;
import org.postgis.jts.JtsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Export nominatim data
 *
 * @author felix, christoph
 */
//@Log4j
public class NominatimSource {
	private final static Logger LOGGER = LoggerFactory.getLogger(NominatimSource.class);
	private final JdbcTemplate template;

	private Importer importer;

	public void setImporter(Importer importer) {
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

	public List<AddressRow> getAddresses(long placeId) {
		return template.query("SELECT place_id, name, class, type, rank_address, admin_level FROM get_addressdata(?) WHERE isaddress ORDER BY rank_address DESC", new Object[]{placeId}, new RowMapper<AddressRow>() {
			@Override
			public AddressRow mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new AddressRow(
						rs.getLong("place_id"),
						Utils.getMap(rs, "name"),
						rs.getString("class"),
						rs.getString("type"),
						rs.getInt("rank_address"),
						rs.getInt("admin_level") // TODO: null check
				);
			}
		});
	}

	public void export() {
		template.query("SELECT " + selectColumns + " FROM placex WHERE linked_place_id IS NULL LIMIT 10; ", new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				PhotonDoc doc = placeRowMapper.mapRow(rs, 0);

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
					} else {
						if(address.isUsefulForContext()) {
							doc.getContext().add(address.getName());
						}
					}
				}

				importer.addDocument(doc);

				try {
					LOGGER.warn(de.komoot.photon.importer.Utils.convert(doc).string());
				} catch(IOException e) {
					LOGGER.error("TODO: add description", e);
				}
				//log.info(doc);
			}
		});

		importer.finish();
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

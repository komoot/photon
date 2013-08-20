package de.komoot.search.importer;

import com.neovisionaries.i18n.CountryCode;
import de.komoot.search.importer.model.OSM_TYPE;
import de.komoot.search.importer.model.I18nName;
import de.komoot.search.importer.model.NominatimEntry;
import de.komoot.search.importer.model.NominatimEntryParent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * handles fetching data from db and queries nominatim's geo index
 *
 * @author christoph
 */
public class IndexCrawler {
	private final static Logger LOGGER = LoggerFactory.getLogger(IndexCrawler.class);

	private final Connection connection;
	private final PreparedStatement statementSingle;
	private final PreparedStatement statementSingleOSM;
	private final PreparedStatement statementAddresses;

	private static final String SQL_HSTORE_NAME = "name->'name' as name, name->'name:en' as name_en, name->'name:fr' as name_fr, name->'name:de' as name_de, name->'name:it' as name_it, name->'ref' as name_ref, name->'place_name' as place_name, name->'short_name' as short_name, name->'official_name' as official_name  ";
	private static final String SQL_TEMPLATE = "SELECT place_id, partition, osm_type, osm_id, class, type, " + SQL_HSTORE_NAME + ", admin_level, housenumber, street, addr_place, isin, postcode, country_code, extratags, st_astext(centroid) as centroid, parent_place_id, linked_place_id, rank_address, rank_search, importance, indexed_status, indexed_date, wikipedia, geometry_sector, calculated_country_code, TRUE as isaddress FROM placex ";

	public IndexCrawler(Connection connection) throws SQLException {
		this.connection = connection;
		this.connection.setAutoCommit(false);

		statementSingle = connection.prepareStatement(SQL_TEMPLATE + " WHERE place_id = ? ");
		statementSingleOSM = connection.prepareStatement(SQL_TEMPLATE + " WHERE osm_id = ? AND osm_type = ? ");
		statementAddresses = connection.prepareStatement("SELECT place_id, osm_type, osm_id, " + SQL_HSTORE_NAME + " , class, type, admin_level, rank_address, isaddress FROM get_addressdata(?) ORDER BY rank_address DESC, isaddress DESC ");
	}

	/**
	 * completes an entry by passing all information provided by entry's parents
	 *
	 * @param entry
	 * @throws SQLException
	 */
	public void completeInformation(NominatimEntry entry) throws SQLException {
		List<NominatimEntryParent> parents = retrieveParents(entry);

		CountryCode country = getCountry(entry, parents);
		if(country == null) {
			LOGGER.error("unexpected: no country was defined for %s", entry);
		}

		// adopt information of parents
		for(NominatimEntryParent addressItem : parents) {
			addressItem.setCountry(country);
			entry.inheritProperties(addressItem);

			// fill places list
			if(addressItem.getPlaceId() != entry.getPlaceId()
					&& addressItem.isCountry() == false && addressItem.isStreet() == false
					&& addressItem.isPostcode() == false && addressItem.getName() != entry.getName()) {

				// handle special case of london
				if(addressItem.getOsmId() == 175342 && OSM_TYPE.R.equals(addressItem.getOsmType())) {
					// this item lies in Greater London
					if(entry.getCity() != null && false == entry.getCity().isNameless()) {
						// move "real" city to places
						entry.addPlace(entry.getCity());
					}
					entry.setCity(I18nName.LONDON);
				}

				if(addressItem.getName() != null && addressItem.getName().isNameless() == false) {
					if(addressItem.isAddress()) {
						entry.addPlace(addressItem.getName());
					} else {
						entry.addSecondaryPlace(addressItem.getName());
					}
				}
			}
		}

		if(entry.getCity() == null || entry.getCity().isNameless()) {
			// no city found in index, take the best match in secondary places

			for(NominatimEntryParent addressItem : parents) {
				if(addressItem.isAddress()) {
					// was already evaluated, there is no city
					if(addressItem.isCity()) {
						LOGGER.info(String.format("you were wrong entry [%s] address item [%s]", entry, addressItem));
					}
					continue;
				}

				if(addressItem.isCity() && false == addressItem.getName().isNameless()) {
					entry.setCity(addressItem.getName());
					break;
				}
			}
		}
	}

	/**
	 * get all parents from nominatim's index
	 *
	 * @param entry
	 * @return
	 * @throws SQLException
	 */
	private List<NominatimEntryParent> retrieveParents(NominatimEntry entry) throws SQLException {
		statementAddresses.setLong(1, entry.getPlaceId());
		ResultSet resultSet = statementAddresses.executeQuery();

		List<NominatimEntryParent> parents = new ArrayList<NominatimEntryParent>();
		while(resultSet.next()) {
			NominatimEntryParent addressItem = new NominatimEntryParent(resultSet);
			parents.add(addressItem);
		}
		return parents;
	}

	/**
	 * get country either from entry itself or from one of its parents
	 *
	 * @param entry
	 * @param parents
	 * @return null if no country information available
	 */
	private CountryCode getCountry(NominatimEntry entry, List<NominatimEntryParent> parents) {
		if(entry.getCountry() != null) {
			return entry.getCountry();
		}

		for(NominatimEntryParent e : parents) {
			if(e.getCountry() != null) {
				return e.getCountry();
			}
		}

		return null;
	}

	/**
	 * get all records for xml conversions
	 *
	 * @param onlyBerlin
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getAllRecords(boolean onlyBerlin) throws SQLException {
		PreparedStatement statementAll;
		String sql = SQL_TEMPLATE + " WHERE osm_type <> 'P' AND (name IS NOT NULL OR housenumber IS NOT NULL OR street IS NOT NULL OR postcode IS NOT NULL) AND centroid IS NOT NULL ";

		if(onlyBerlin) {
			sql += "AND st_contains(ST_GeomFromText('POLYGON ((12.718964 52.880734,13.92746 52.880734,13.92746 52.160455,12.718964 52.160455,12.718964 52.880734))', 4326), centroid) ";
		}

		sql += " ORDER BY st_x(ST_SnapToGrid(centroid, 0.1)), st_y(ST_SnapToGrid(centroid, 0.1)) "; // should be nice for performance reasons ...?

		statementAll = connection.prepareStatement(sql);
		statementAll.setFetchSize(100000);

		return statementAll.executeQuery();
	}

	/**
	 * searches for a single entry for a given osm id
	 *
	 * @param osmId
	 * @param osmType
	 * @return null if entry of OSM id cannot be found
	 * @throws SQLException
	 */
	public NominatimEntry getSingleOSM(long osmId, String osmType) throws SQLException {
		statementSingleOSM.setLong(1, osmId);
		statementSingleOSM.setString(2, osmType);
		ResultSet resultSet = statementSingleOSM.executeQuery();

		if(resultSet.next()) {
			NominatimEntry entry = new NominatimEntry(resultSet);
			completeInformation(entry);
			return entry;
		} else {
			return null;
		}
	}
}

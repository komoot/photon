package de.komoot.photon.importer.model;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * this class represents an osm object that can be a city, an address, street, park ... the osm objects do not obtain
 * all information necessary to create a solr document. missing information will be inherited from parents {@link
 * NominatimEntryParent}.
 *
 * @author christoph
 */
public class NominatimEntry extends NominatimEntryParent {
	protected final static Logger LOGGER = LoggerFactory.getLogger(NominatimEntry.class);
	protected final static WKTReader wktReader = new WKTReader();
	protected Geometry centroid;
	protected double importance;
	protected Long parentId;
	protected int rankSearch;
	protected List<I18nName> places = new ArrayList<>();

	/** constructor only used for testing */
	public NominatimEntry() {
	}

	public NominatimEntry(ResultSet res, List<String> languages) {
		super(res, languages);
		try {
			this.countryCode = res.getString("country_code");
			this.calculatedCountryCode = res.getString("calculated_country_code");
			String c = res.getString("centroid");
			this.centroid = wktReader.read(c);
			this.houseNumber = res.getString("housenumber");
			this.importance = res.getDouble("importance");

			String streetColumn = res.getString("street");
			if(streetColumn != null) {
				this.street = streetColumn;
			}

			this.parentId = res.getLong("parent_place_id");
			if(parentId == 0) {
				this.parentId = null;
			}

			this.postcode = res.getString("postcode");
			this.rankSearch = res.getInt("rank_search");
		} catch(Exception e) {
			LOGGER.error(String.format("reading place [%d]", this.placeId), e);
		}

		// check country code
		if(this.countryCode != null) {
			this.country = CountryCode.getByCode(this.countryCode);
		}
		if(this.country == null && this.calculatedCountryCode != null) {
			this.country = CountryCode.getByCode(this.calculatedCountryCode);
		}

		updateAddressInformation();
	}

	public Geometry getCentroid() {
		return centroid;
	}

	public void setCentroid(Geometry centroid) {
		this.centroid = centroid;
	}

	public I18nName getCity() {
		return city;
	}

	public void setCity(I18nName city) {
		this.city = city;
	}

	public String getHousenumber() {
		return houseNumber;
	}

	public List<I18nName> getPlaces() {
		return this.places;
	}

	public String getPostcode() {
		return postcode;
	}

	public int getRankAddress() {
		return rankAddress;
	}

	public int getRankSearch() {
		return rankSearch;
	}

	public String getStreet() {
		return street;
	}

	@Override
	public String toString() {
		return String.format("NominatimEntry [%s] (%d, %d (%s), %s -> %s)", name != null && name.getName() != null ? name.getName() : "-noname-", placeId, osmId, osmType, osmKey, osmValue);
	}

	public void addPlace(I18nName place) {
		this.places.add(place);
	}

	/**
	 * the coordinate of the entry (centroid of the geometry)
	 *
	 * @return
	 */
	public Coordinate getCoordinate() {
		return this.centroid.getCentroid().getCoordinate();
	}

	/**
	 * get type
	 *
	 * @return can be null if entry is not associated with a type
	 */
	public ENTRY_TYPE getType() {
		if(isCity()) {
			return ENTRY_TYPE.CITY;
		}

		if(isCountry()) {
			return ENTRY_TYPE.COUNTRY;
		}

		if(isStreet()) {
			return ENTRY_TYPE.STREET;
		}

		return null;
	}

	/**
	 * inherits properties from parent that were not already set, like country, city, ...
	 *
	 * @param addressItem
	 */
	public void inheritProperties(NominatimEntryParent addressItem) {
		inheritFields(addressItem);
		updateAddressInformation();
	}

	/**
	 * adopt information of parent
	 *
	 * @param parent
	 */
	public void inheritFields(NominatimEntryParent parent) {
		if(country == null) {
			country = parent.country;
		}
		if(city == null || city.isNameless()) {
			city = parent.city;
		}
		if(street == null) {
			street = parent.street;
		}
		if(houseNumber == null) {
			houseNumber = parent.houseNumber;
		}
		if(postcode == null) {
			postcode = parent.postcode;
		}
	}
}

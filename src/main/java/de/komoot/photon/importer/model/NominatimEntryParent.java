package de.komoot.photon.importer.model;

import com.neovisionaries.i18n.CountryCode;
import de.komoot.photon.importer.InternationalAdminLevel;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * parent of a nominatim entry that has only a subset of fields of its child.
 * <p/>
 * e.g.: http://nominatim.openstreetmap.org/details.php?place_id=3676877422. nominatim entry is komoot HQ, parents are
 * the address lines at the bottom
 *
 * @author christoph
 * @date 20.08.13
 */
public class NominatimEntryParent {
	protected int adminLevel;
	protected String calculatedCountryCode;
	protected String countryCode;
	protected String houseNumber;
	protected String osmKey;
	protected I18nName name;
	protected long osmId;
	protected OSM_TYPE osmType;
	protected long placeId;
	protected String postcode;
	protected int rankAddress;
	protected String street;
	protected String osmValue;
	protected CountryCode country;
	protected I18nName city;

	/** constructor only used for testing */
	public NominatimEntryParent() {
	}

	public NominatimEntryParent(ResultSet res) {
		try {
			this.adminLevel = res.getInt("admin_level");
			this.placeId = res.getLong("place_id");

			this.osmId = res.getLong("osm_id");
			this.osmType = OSM_TYPE.get(res.getString("osm_type"));

			this.osmKey = res.getString("class");
			this.osmValue = res.getString("type");

			this.name = new I18nName(res.getString("name"), res.getString("name_de"), res.getString("name_en"), res.getString("name_fr"), res.getString("name_it"));

			if(this.name.isNameless()) {
				this.name = new I18nName(res.getString("place_name"), null, null, null, null);

				if(this.name.isNameless()) {
					this.name = new I18nName(res.getString("short_name"), null, null, null, null);

					if(this.name.isNameless()) {
						this.name = new I18nName(res.getString("official_name"), null, null, null, null);
					}
				}
			}

			this.rankAddress = res.getInt("rank_address");

			if(isCountry()) {
				this.countryCode = res.getString("name_ref");
				if(false == StringUtils.hasText(this.countryCode)) {
					NominatimEntry.LOGGER.info("unexpected: no country code, %s", this);
				}
			}

			if(isPostcode()) {
				if(name.locale != null) {
					this.postcode = name.locale;
				} else {
					this.postcode = res.getString("name_ref");
				}
			}

			if(isStreet()) {
				if(name.locale != null) {
					street = name.locale;
				}
			}

			if(getHouseNumber()) {
				this.houseNumber = res.getString("name_ref");
			}
		} catch(SQLException e) {
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
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

	public boolean isCountry() {
		if("country_code".equals(osmValue) && "place".equals(osmKey)) {
			return true;
		}

		// decide by admin level
		if(country == null) {
			return false;
		}
		AdminScheme al = InternationalAdminLevel.get(country);
		return this.adminLevel == al.country;
	}

	public boolean isPostcode() {
		return ("postcode".equals(osmValue) && "place".equals(osmKey)) || ("postal_code".equals(osmValue) && "boundary".equals(osmKey));
	}

	public boolean isStreet() {
		return "highway".equals(this.osmKey);
	}

	public boolean getHouseNumber() {
		return "house_number".equals(osmValue) && "place".equals(osmKey);
	}

	public void updateAddressInformation() {
		if(isCity()) {
			if(this.name.isNameless()) {
				NominatimEntry.LOGGER.warn(String.format("Unexpected data: a city does not have a name [%s]", this));
			} else {
				this.city = this.name;
			}
		}
	}

	/**
	 * checks if entry represents a city
	 *
	 * @return
	 */
	public boolean isCity() {
		if("place".equals(osmKey) && ("city".equals(osmValue) || "town".equals(osmValue) || "village".equals(osmValue))) {
			return true;
		}

		// decide by admin level
		if(country == null) {
			return false;
		}
		AdminScheme al = InternationalAdminLevel.get(country);
		if(this.adminLevel == al.city) {
			return true;
		}

		if(InternationalAdminLevel.isCityByException(this)) {
			return true;
		}

		return false;
	}

	public int getAdminLevel() {
		return adminLevel;
	}

	public CountryCode getCountry() {
		return country;
	}

	public I18nName getName() {
		return name;
	}

	public void setName(I18nName name) {
		this.name = name;
	}

	public long getOsmId() {
		return osmId;
	}

	public void setOsmId(long osmId) {
		this.osmId = osmId;
	}

	public String getOsmKey() {
		return osmKey;
	}

	public OSM_TYPE getOsmType() {
		return osmType;
	}

	public String getOsmValue() {
		return osmValue;
	}

	public long getPlaceId() {
		return placeId;
	}

	public void setPlaceId(long placeId) {
		this.placeId = placeId;
	}

	public void setCountry(CountryCode country) {
		this.country = country;
		this.updateAddressInformation();
	}
}

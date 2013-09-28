package de.komoot.photon.importer;

import com.neovisionaries.i18n.CountryCode;
import de.komoot.photon.importer.model.AdminScheme;
import de.komoot.photon.importer.model.CityException;
import de.komoot.photon.importer.model.NominatimEntryParent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * User: christoph Date: 25.07.13
 * <p/>
 * mapping class that provides information about local mapping schemes for administrative boundaries see
 * http://wiki.openstreetmap.org/wiki/Tag:boundary%3Dadministrative
 */
public class InternationalAdminLevel {
	private final static Logger LOGGER = LoggerFactory.getLogger(InternationalAdminLevel.class);

	private static Map<CountryCode, AdminScheme> map = new HashMap<CountryCode, AdminScheme>();
	private static AdminScheme fallback = new AdminScheme(2, 8);

	static {
		map.put(CountryCode.AL, new AdminScheme(2, 8));
		map.put(CountryCode.AD, new AdminScheme(2, 8));
		map.put(CountryCode.BE, new AdminScheme(2, 8));
		map.put(CountryCode.BA, new AdminScheme(2, 6));
		map.put(CountryCode.DK, new AdminScheme(2, 8));
		map.put(CountryCode.DE, new AdminScheme(2, 8));
		map.put(CountryCode.ES, new AdminScheme(2, 8));
		map.put(CountryCode.FI, new AdminScheme(2, 8));
		map.put(CountryCode.FR, new AdminScheme(2, 8));
		map.put(CountryCode.IE, new AdminScheme(2, 7));
		map.put(CountryCode.IT, new AdminScheme(2, 8));
		map.put(CountryCode.HR, new AdminScheme(2, 7));
		map.put(CountryCode.LV, new AdminScheme(2, 7));
		map.put(CountryCode.LI, new AdminScheme(2, 8));
		map.put(CountryCode.LU, new AdminScheme(2, 8));
		map.put(CountryCode.NL, new AdminScheme(2, 8));
		map.put(CountryCode.NO, new AdminScheme(2, 7));
		map.put(CountryCode.PL, new AdminScheme(2, 8));
		map.put(CountryCode.AT, new AdminScheme(2, 8));
		map.put(CountryCode.PT, new AdminScheme(2, 7));
		map.put(CountryCode.RU, new AdminScheme(2, 8));
		map.put(CountryCode.SE, new AdminScheme(2, 7));
		map.put(CountryCode.CH, new AdminScheme(2, 8));
		map.put(CountryCode.SK, new AdminScheme(2, 8));
		map.put(CountryCode.ES, new AdminScheme(2, 8));
		map.put(CountryCode.CZ, new AdminScheme(2, 8));
		map.put(CountryCode.HU, new AdminScheme(2, 8));
		map.put(CountryCode.GB, new AdminScheme(2, 8));

		// added by christoph
		map.put(CountryCode.IS, new AdminScheme(2, 8));
		map.put(CountryCode.JE, new AdminScheme(2, 8)); // like uk
		map.put(CountryCode.SI, new AdminScheme(2, 8));
		map.put(CountryCode.MC, new AdminScheme(2, 8)); // like france
		map.put(CountryCode.GG, new AdminScheme(2, 8)); // like uk
		map.put(CountryCode.IM, new AdminScheme(2, 8)); // like uk
		map.put(CountryCode.RS, new AdminScheme(2, 8));
		map.put(CountryCode.US, new AdminScheme(2, 8));
	}

	private static Map<CountryCode, Set<CityException>> cityExceptions = new HashMap<CountryCode, Set<CityException>>();

	static {
		Set<CityException> exceptions = new HashSet<CityException>(Arrays.asList(new CityException(4, "Hamburg"), new CityException(4, "Berlin"), new CityException(6, "Bremen")));
		cityExceptions.put(CountryCode.DE, exceptions);
	}

	public static AdminScheme get(CountryCode countryCode) {
		if(countryCode == null) {
			return fallback;
		}

		AdminScheme adminScheme = map.get(countryCode);
		if(adminScheme == null) {
			LOGGER.warn(String.format("unknown country code %s (%s), taking fall back admin level", countryCode, countryCode.getName()));
			map.put(countryCode, fallback);
			return fallback;
		}

		return adminScheme;
	}

	/**
	 * returns true if entry meets special requirement, normally for very big cities (berlin, london, ...)
	 *
	 * @param entry
	 * @return
	 */
	public static boolean isCityByException(NominatimEntryParent entry) {
		Set<CityException> exceptions = cityExceptions.get(entry.getCountry());

		if(exceptions == null) {
			return false;
		}

		for(CityException exception : exceptions) {
			if(exception.adminLevel == entry.getAdminLevel() && exception.name.equals(entry.getName().getName())) {
				return true;
			}
		}

		return false;
	}
}


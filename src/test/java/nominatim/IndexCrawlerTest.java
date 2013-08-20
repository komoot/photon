package nominatim;

import com.neovisionaries.i18n.CountryCode;
import de.komoot.search.importer.NominatimImporter;
import de.komoot.search.importer.model.I18nName;
import de.komoot.search.importer.model.NominatimEntry;
import org.junit.*;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author christoph
 */
public class IndexCrawlerTest extends Constants {

	private NominatimImporter importer;

	@Before
	public void setUp() throws Exception {
		importer = new NominatimImporter("localhost", 10008, "nominatim_eu2", "christoph", "christoph", new File("/dev/null"));
	}

	@Test
	public void testAustria() throws Exception {
		assertEntry(134416672, "W", name("Dr. Elmar Lingg"), "Bödmerstraße", "7", "6993", name("Mittelberg"), CountryCode.AT, VORARLBERG, null, BEZIRK_BREGENZ, null);
		assertEntry(29475612, "W", name("Jaghausen"), "Jaghausen", null, "6883", name("Au"), CountryCode.AT, VORARLBERG, null, BEZIRK_BREGENZ, null);
		assertEntry(51296546, "W", NULL_NAME, "Alte Landstraße", "6", "6923", name("Lauterach"), CountryCode.AT, VORARLBERG, null, BEZIRK_BREGENZ, null);
	}

	@Test
	public void testGermany() throws Exception {
		assertEntry(1552576044, "N", NULL_NAME, "Dircksenstraße", "51", "10178", BERLIN, CountryCode.DE, MITTE);
		assertEntry(48664717, "W", name("Dircksenstraße"), "Dircksenstraße", null, "10179", BERLIN, CountryCode.DE, name("Mitte"));
		assertEntry(4613096, "W", name("Dircksenstraße"), "Dircksenstraße", null, "10179", BERLIN, CountryCode.DE, name("Mitte"));
		assertEntry(2229764925l, "N", NULL_NAME, "Söllerpfad", "1", "13465", BERLIN, CountryCode.DE, name("Reinickendorf"));
		assertEntry(62422, "R", BERLIN, null, null, null, BERLIN, CountryCode.DE);
	}

	@Test
	public void testUK() throws SQLException {
		assertEntry(369982960, "N", name("Meeting Point Internet cafe"), "Stoke Newington High Street", "74", "N16 8EL", LONDON, CountryCode.GB, ENGLAND, name("London Borough of Hackney")); // eigentlich london als city, plz: N16 7PA laut google
		assertEntry(23599778, "W", name("Gateway Green"), null, null, "B10", name("Birmingham"), CountryCode.GB, ENGLAND); // laut google maps b10 als zip
		assertEntry(140471631, "W", name("10 Downing Street"), "Downing Street", "10", "SW1A 2AA", LONDON, CountryCode.GB, ENGLAND);
		assertEntry(118362, "R", name("Leeds"), null, null, null, name("Leeds"), CountryCode.GB, ENGLAND, name("United Kindom"), name("West Yorkshire"), name("Yorkshire and the Humber"));
	}

	@Test
	public void testFrance() throws SQLException {
		assertEntry(40230383, "W", name("Rue Nicolas Midant"), "Rue Nicolas Midant", null, "21850", name("Ruffey-lès-Echirey"), CountryCode.FR, new I18nName(null, "Burgund", null, null, null));
		assertEntry(821484087, "N", name("Monument aux Morts"), "Place de l'Église", null, "74120", name("Megève"), CountryCode.FR, name("Haute-Savoie"));
		assertEntry(1733466045, "N", NULL_NAME, "Rue Joseph Moyse", "5", "44100", new I18nName("Nantes", null, null, "Nantes", null), CountryCode.FR, name("Pays de la Loire"));
	}

	public void assertEntry(long osmId, String osmType, I18nName name, String street, String housenumber, String postcode, I18nName city, CountryCode country, I18nName... regions) throws SQLException {
		NominatimEntry entry = importer.getSingleEntry(osmId, osmType);
		assertNotNull(String.format("could not found [%d][%s]", osmId, osmType), entry);

		assertEqualsName(name, entry.getName(), "name, " + entry + "]");
		assertEqualsName(city, entry.getCity(), "city, " + entry + "]");
		assertEquals(country, entry.getCountry());
		assertEquals(street, entry.getStreet());
		assertEquals(housenumber, entry.getHousenumber());
		assertEquals(postcode, entry.getPostcode());

		for(I18nName region : regions) {
			if(region != null) {
				boolean contains = contains(entry.getPlaces(), region);
				assertTrue(String.format("missing place [%s] in item [%s]", region, entry), contains);
			}
		}
	}

	private boolean contains(List<I18nName> regions, I18nName expected) {
		for(I18nName actual : regions) {
			if(contains(expected, actual)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * check if contains function work as expected
	 *
	 * @throws Exception
	 */
	@Test
	public void testContains() throws Exception {
		List<I18nName> regions = Arrays.asList(new I18nName(), new I18nName());
		I18nName region = new I18nName(null, "regionen name", null, null, null);
		assertFalse(contains(regions, region));

		regions = Arrays.asList(new I18nName(), new I18nName(null, null, "my darling", "oui oui cherie", "mi amore"));
		region = new I18nName(null, null, null, "oui oui cherie", null);
		assertTrue(contains(regions, region));
	}

	private boolean contains(I18nName expected, I18nName actual) {
		if(expected.locale != null && false == expected.locale.equals(actual.locale)) {
			return false;
		}
		if(expected.de != null && false == expected.de.equals(actual.de)) {
			return false;
		}
		if(expected.en != null && false == expected.en.equals(actual.en)) {
			return false;
		}
		if(expected.fr != null && false == expected.fr.equals(actual.fr)) {
			return false;
		}
		if(expected.it != null && false == expected.it.equals(actual.it)) {
			return false;
		}

		return true;
	}

	public void assertEqualsName(I18nName expected, I18nName actual, String msg) {
		if(expected == null || actual == null) {
			assertEquals(msg, expected, actual);
			return;
		}

		assertEquals(msg, expected.isNameless(), actual.isNameless());

		if(expected.locale != null)
			assertEquals(msg, expected.locale, actual.locale);
		if(expected.de != null)
			assertEquals(msg, expected.de, actual.de);
		if(expected.en != null)
			assertEquals(msg, expected.en, actual.en);
		if(expected.fr != null)
			assertEquals(msg, expected.fr, actual.fr);
		if(expected.it != null)
			assertEquals(msg, expected.it, actual.it);
	}
}
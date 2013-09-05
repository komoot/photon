package de.komoot.photon.importer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import de.komoot.photon.importer.model.I18nName;
import de.komoot.photon.importer.model.NominatimEntry;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

/**
 * @author christoph
 */
public class XMLOutputTest extends Constants {
	private final static Logger LOGGER = LoggerFactory.getLogger(XMLOutputTest.class);

	private ByteArrayOutputStream outputStream;
	private XMLWriter cut;
	private SAXParserFactory spf = SAXParserFactory.newInstance();

	@Test
	public void testNormal() throws Exception {
		String inputName = "Ayuntamiento";
		String expectedName = inputName;
		int osmId = 2;
		long placeId = 1l;

		check(inputName, expectedName, osmId, placeId);
	}

	@Test
	public void testUTF16_1() throws Exception {
		String inputName = "wecj 𠀀asdf"; // or whatever this is
		String expectedName = "asdf";
		int osmId = 20;
		long placeId = 10l;

		check(inputName, expectedName, osmId, placeId);
	}

	//@Test
	public void testUTF16_2() throws Exception {
		String inputName = "Ayuntamiento 🏤"; // or whatever this is
		String expectedName = "Ayuntamiento";
		int osmId = 20;
		long placeId = 10l;

		check(inputName, expectedName, osmId, placeId);
	}

	//@Test
	public void testUTF8Normal() throws Exception {
		String[] arr = new String[]{"äö", "äöäöä", "¡", "}}{|]"};

		for(String inputName : arr) {
			LOGGER.info("test: " + inputName);
			String expectedName = inputName;
			int osmId = 20;
			long placeId = 10l;

			check(inputName, expectedName, osmId, placeId);
		}
	}

	@Test
	public void testUTF8Freaky() throws Exception {
		String[] arr = new String[]{"₰", "✝", "⚔", "❡", "✪", "⫍", "ȵ", "|{}≠¿", "«∑€†Ω¨⁄øπ"};

		for(String inputName : arr) {
			LOGGER.info("test: " + inputName);
			String expectedName = inputName;
			int osmId = 20;
			long placeId = 10l;

			check(inputName, expectedName, osmId, placeId);
		}
	}

	@Test
	public void testLong() throws Exception {
		String inputName = "test";
		String expectedName = inputName;
		long osmId = Long.MAX_VALUE;
		long placeId = Long.MIN_VALUE;

		check(inputName, expectedName, osmId, placeId);
	}

	@Test
	public void testUmlaut() throws Exception {
		String inputName = "ä";
		String expectedName = inputName;
		long osmId = 17;
		long placeId = 11;

		check(inputName, expectedName, osmId, placeId);
	}

	private void check(String inputName, String expectedName, long osmId, long placeId) throws Exception {
		NominatimEntry entry = new NominatimEntry();
		entry.setOsmId(osmId);
		entry.setPlaceId(placeId);
		entry.setName(new I18nName(inputName, null, null, null, null));

		List<XMLModel> models = get(entry);

		assertEquals(1, models.size());
		XMLModel m = models.get(0);

		assertEquals("osm id not right", osmId, m.getOsmId());
		assertEquals("id not right", placeId, (long) m.getId());
		assertEquals("name not right", expectedName, m.getName());
	}

	public List<XMLModel> get(NominatimEntry entry) throws Exception {
		if(entry.getCentroid() == null) {
			// prevent NPE
			entry.setCentroid(new Point(new Coordinate(0, 0), new PrecisionModel(), 4326));
		}

		outputStream = new ByteArrayOutputStream();

		cut = new XMLWriter(outputStream);
		cut.write(entry);
		cut.finish();

		SAXParser sp = spf.newSAXParser();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

		NominatimXMLHandler handler = new NominatimXMLHandler();

		GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
		sp.parse(gzipInputStream, handler);

		// test dump to file
		inputStream.reset();
		OutputStream outputStream1 = new FileOutputStream(new File("/tmp/nominatim_temp.xml"));
		IOUtils.copy(inputStream, outputStream1);

		return handler.getEntries();
	}
}
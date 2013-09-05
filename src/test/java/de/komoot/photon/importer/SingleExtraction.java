package de.komoot.photon.importer;

import de.komoot.photon.importer.model.NominatimEntry;
import org.junit.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * @author christoph
 * @date 15.08.13
 */
public class SingleExtraction {
	@Test
	public void getSingleEntry() throws SQLException, IOException, XMLStreamException {
		File file = new File("/Users/christoph/Desktop/single.xml.gz");
		NominatimImporter importer = new NominatimImporter("localhost", 10008, "nominatim_eu2", "christoph", "christoph", file);
		NominatimEntry entry = importer.getSingleEntry(37736073, "W");

		if(entry == null) {
			fail("could not find item");
		}

		XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(file));
		xmlWriter.write(entry);
		xmlWriter.finish();
	}
}

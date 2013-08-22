package de.komoot.search.importer;

import de.komoot.search.importer.model.NominatimEntry;
import org.junit.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author christoph
 */
public class NominatimXMLHandler extends DefaultHandler {
	private XMLModel currentEntry;
	private String currentValue;

	private List<XMLModel> entries = new ArrayList<XMLModel>();
	private String lastFieldName;

	public List<XMLModel> getEntries() {
		return entries;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(qName.equals("doc")) {
			currentEntry = new XMLModel();
			entries.add(currentEntry);
		}

		if(qName.equals("field")) {
			String name = attributes.getValue("name");
			if("id".equals(name) || "osm_id".equals(name) || "name".equals(name)) {
				lastFieldName = name;
			} else {
				lastFieldName = null;
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(lastFieldName == null)
			return;

		if(lastFieldName.equals("id")) {
			currentEntry.setId(Long.parseLong(currentValue));
		} else if(lastFieldName.equals("osm_id")) {
			currentEntry.setOsmId(Long.parseLong(currentValue));
		} else if(lastFieldName.equals("name")) {
			currentEntry.setName(currentValue.trim());
		}

		lastFieldName = null;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue = new String(ch, start, length);
	}

	/**
	 * @author christoph
	 * @date 15.08.13
	 */
	public static class SingleExtraction {
		@Test
		public void getSingleEntry() throws SQLException, IOException, XMLStreamException {
			File file = new File("/Users/christoph/Desktop/single.xml.gz");
			NominatimImporter importer = new NominatimImporter("localhost", 10008, "nominatim_eu2", "christoph", "christoph", file);
			NominatimEntry entry = importer.getSingleEntry(149810698, "W");

			if(entry == null) {
				fail("could not find item");
			}

			XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(file));
			xmlWriter.write(entry);
			xmlWriter.finish();
		}
	}
}

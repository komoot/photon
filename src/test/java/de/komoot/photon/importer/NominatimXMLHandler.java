package de.komoot.photon.importer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

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
}

package de.komoot.search.importer;

import com.google.common.base.Joiner;
import com.vividsolutions.jts.geom.Coordinate;
import de.komoot.search.importer.model.ENTRY_TYPE;
import de.komoot.search.importer.model.I18nName;
import de.komoot.search.importer.model.NominatimEntry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

/**
 * class that dumps nominatim entries to xml
 *
 * @author christoph
 */
public class XMLWriter {
	private OutputStreamWriter out;
	private XMLStreamWriter writer;
	private Joiner joiner = Joiner.on(", ");
	private XMLOutputFactory factory = XMLOutputFactory.newInstance();

	public XMLWriter(OutputStream outputStream) {
		try {
			out = new OutputStreamWriter(new GZIPOutputStream(outputStream), "UTF-8");
			writer = factory.createXMLStreamWriter(out);
			writer.writeStartDocument("utf-8", "1.0");
			writer.writeStartElement("add");
		} catch(IOException e) {
			throw new RuntimeException("cannot create solr xml file", e);
		} catch(XMLStreamException e) {
			throw new RuntimeException("cannot create solr xml file", e);
		}
	}

	public void write(NominatimEntry entry) throws IOException, XMLStreamException {
		writer.writeStartElement("doc");

		write("id", entry.getPlaceId());

		Coordinate centroid = entry.getCoordinate();
		write("coordinate", String.format(Locale.ENGLISH, "%f,%f", centroid.y, centroid.x));

		write("osm_id", entry.getOsmId());

		write("name", entry.getName());

		// Address
		write("street", entry.getStreet());
		write("housenumber", entry.getHousenumber());
		write("postcode", entry.getPostcode());

		write("country", InternationalCountryName.get(entry.getCountry()));
		write("city", entry.getCity());

		write("places", entry.getPlaces());
		write("secondary_places", entry.getSecondaryPlaces());

		write("ranking_kmt", 30 - entry.getRankSearch());

		ENTRY_TYPE type = entry.getType();
		write("type", type == null ? null : type.toString());

		writer.writeEndElement();
	}

	private void write(String name, I18nName n) throws IOException, XMLStreamException {
		if(n == null) {
			n = new I18nName();
		}
		write(name, n.locale);
		write(name + "_de", n.de);
		write(name + "_en", n.en);
		//        write(name + "_fr", n.fr);
		//        write(name + "_it", n.it);
	}

	private void write(String fieldName, List<I18nName> names) throws IOException, XMLStreamException {
		List<String> locale = new ArrayList<String>(names.size());
		List<String> de = new ArrayList<String>(names.size());
		List<String> en = new ArrayList<String>(names.size());
		//        List<String> fr = new ArrayList<String>(names.size());
		//        List<String> it = new ArrayList<String>(names.size());

		for(I18nName name : names) {
			if(name.locale != null) locale.add(name.locale);
			if(name.de != null) de.add(name.de);
			if(name.en != null) en.add(name.en);
			//            if (name.fr != null) fr.add(name.fr);
			//            if (name.it != null) it.add(name.it);
		}

		writeList(fieldName, locale, "");
		writeList(fieldName, de, "_de");
		writeList(fieldName, en, "_en");
		//        writeList(fieldName, fr, "_fr");
		//        writeList(fieldName, it, "_it");
	}

	private void writeList(String fieldName, List<String> list, String suffix) throws XMLStreamException, IOException {
		if(list.size() > 0) {
			write(fieldName + suffix, joiner.join(list));
		} else {
			write(fieldName + suffix, (String) null);
		}
	}

	private void write(String name, long value) throws IOException, XMLStreamException {
		write(name, Long.toString(value));
	}

	private void write(String name, Object value) throws IOException, XMLStreamException {
		if(value == null) {
			return;
		}

		writer.writeStartElement("field");
		writer.writeAttribute("name", name);
		writer.writeCharacters(value.toString());
		writer.writeEndElement();
	}

	public void finish() throws XMLStreamException, IOException {
		writer.writeEndElement();
		writer.writeEndDocument();
		writer.flush();
		writer.close();
		out.close();
	}
}

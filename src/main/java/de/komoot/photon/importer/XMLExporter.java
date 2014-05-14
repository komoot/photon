package de.komoot.photon.importer;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.vividsolutions.jts.geom.Coordinate;
import de.komoot.photon.importer.model.I18nName;
import de.komoot.photon.importer.model.NominatimEntry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * class that dumps nominatim entries to xml
 *
 * @author christoph
 */
public class XMLExporter implements Exporter {
	private OutputStreamWriter out;
	private XMLStreamWriter writer;
	private Joiner joiner = Joiner.on(", ");
	private XMLOutputFactory factory = XMLOutputFactory.newInstance();

	public XMLExporter(OutputStream outputStream) {
		try {
			out = new OutputStreamWriter(new GZIPOutputStream(outputStream), "UTF-8");
			writer = factory.createXMLStreamWriter(out);
			writer.writeStartDocument("utf-8", "1.0");
			writer.writeStartElement("add");
		} catch(IOException | XMLStreamException e) {
			throw new RuntimeException("cannot create solr xml file", e);
		}
	}

	@Override
	public void write(NominatimEntry entry) {
		try {
			writer.writeStartElement("doc");
			write("id", entry.getPlaceId());

			Coordinate centroid = entry.getCoordinate();
			write("coordinate", String.format(Locale.ENGLISH, "%f,%f", centroid.y, centroid.x));

			write("osm_id", entry.getOsmId());
			write("osm_type", entry.getOsmType() == null ? "" : entry.getOsmType().name());

			write("name", entry.getName());

			write("osm_key", entry.getOsmKey());
			write("osm_value", entry.getOsmValue());

			// Address
			write("street", entry.getStreet());
			write("housenumber", entry.getHousenumber());
			write("postcode", entry.getPostcode());

			write("country", InternationalCountryName.get(entry.getCountry()));
			write("city", entry.getCity());

			write("places", entry.getPlaces());

			write("ranking", 30 - entry.getRankSearch());
			write("importance", entry.getImportance());

			writer.writeEndElement();
		} catch(XMLStreamException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void write(String name, I18nName n) throws IOException, XMLStreamException {
		if(n == null) {
			n = new I18nName();
		}

		write(name, n.getName());
		for(Map.Entry<String, String> entry : n.getTranslations().entrySet()) {
			write(name + "_" + entry.getKey(), entry.getValue());
		}
	}

	private void write(String fieldName, List<I18nName> iNames) throws IOException, XMLStreamException {
		List<String> names = new ArrayList<>(iNames.size());
		HashMultimap<String, String> translations = HashMultimap.create();

		for(I18nName name : iNames) {
			if(name.getName() != null) {
				names.add(name.getName());
			}

			for(Map.Entry<String, String> entry : name.getTranslations().entrySet()) {
				String language = entry.getKey();
				String translation = entry.getValue();
				translations.put(language, translation);
			}
		}

		writeList(fieldName, names, "");

		for(String language : translations.keySet()) {
			writeList(fieldName, translations.get(language), "_" + language);
		}
	}

	private void writeList(String fieldName, Collection<String> list, String suffix) throws XMLStreamException, IOException {
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

	@Override
	public void finish() {
		try {
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.flush();
			writer.close();
			out.close();
		} catch(XMLStreamException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}

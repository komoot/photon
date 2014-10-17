package de.komoot.photon.importer;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.vividsolutions.jts.geom.Envelope;
import de.komoot.photon.importer.model.PhotonDoc;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * helper functions to create convert a photon document to XContentBuilder object / JSON
 *
 * @author christoph
 */
public class Utils {
	static final Joiner commaJoiner = Joiner.on(", ").skipNulls();

	final static String[] languages = new String[]{"de", "en", "fr", "it"};

	public static XContentBuilder convert(PhotonDoc doc) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
				.field(Tags.KEY_OSM_ID, doc.getOsmId())
				.field(Tags.KEY_OSM_TYPE, doc.getOsmType())
				.field(Tags.KEY_OSM_KEY, doc.getTagKey())
				.field(Tags.KEY_OSM_VALUE, doc.getTagValue())
				.field(Tags.KEY_IMPORTANCE, doc.getImportance());

		if(doc.getCentroid() != null) {
			builder.startObject("coordinate")
					.field("lat", doc.getCentroid().getY())
					.field("lon", doc.getCentroid().getX())
					.endObject();
		}

		if(doc.getHouseNumber() != null) {
			builder.field("housenumber", doc.getHouseNumber());
		}

		if(doc.getPostcode() != null) {
			builder.field("postcode", doc.getPostcode());
		}

		writeName(builder, doc.getName());
		writeIntlNames(builder, doc.getCity(), "city");
		writeIntlNames(builder, doc.getCountry(), "country");
		writeIntlNames(builder, doc.getState(), "state");
		writeIntlNames(builder, doc.getStreet(), "street");
		writeContext(builder, doc.getContext());
		writeExtent(builder, doc.getBbox());

		return builder;
	}

	private static void writeExtent(XContentBuilder builder, Envelope bbox) throws IOException {
		if(bbox == null) return;

		if(bbox.getArea() == 0.) return;

		// http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_envelope
		builder.startObject("extent");
		builder.field("type", "envelope");

		builder.startArray("coordinates");
		builder.startArray().value(bbox.getMinX()).value(bbox.getMaxY()).endArray();
		builder.startArray().value(bbox.getMaxX()).value(bbox.getMinY()).endArray();

		builder.endArray();
		builder.endObject();
	}

	private static void writeName(XContentBuilder builder, Map<String, String> name) throws IOException {
		Map<String, String> fNames = filterNames(name);

		if(name.get("alt_name") != null)
			fNames.put("alt", name.get("alt_name"));

		if(name.get("int_name") != null)
			fNames.put("int", name.get("int_name"));

		if(name.get("loc_name") != null)
			fNames.put("loc", name.get("loc_name"));

		if(name.get("old_name") != null)
			fNames.put("old", name.get("old_name"));

		write(builder, fNames, "name");
	}

	private static void write(XContentBuilder builder, Map<String, String> fNames, String name) throws IOException {
		if(fNames.isEmpty()) return;

		builder.startObject(name);
		for(Map.Entry<String, String> entry : fNames.entrySet()) {
			builder.field(entry.getKey(), entry.getValue());
		}
		builder.endObject();
	}

	protected static void writeContext(XContentBuilder builder, Set<Map<String, String>> contexts) throws IOException {
		final SetMultimap<String, String> multimap = HashMultimap.create();

		for(Map<String, String> context : contexts) {
			if(context.get("name") != null) {
				multimap.put("default", context.get("name"));
			}
		}

		for(String language : languages) {
			for(Map<String, String> context : contexts) {
				if(context.get("name:" + language) != null) {
					multimap.put(language, context.get("name:" + language));
				}
			}
		}

		final Map<String, Collection<String>> map = multimap.asMap();
		if(!multimap.isEmpty()) {
			builder.startObject("context");
			for(Map.Entry<String, Collection<String>> entry : map.entrySet()) {
				builder.field(entry.getKey(), commaJoiner.join(entry.getValue()));
			}
			builder.endObject();
		}
	}

	private static void writeIntlNames(XContentBuilder builder, Map<String, String> names, String name) throws IOException {
		Map<String, String> fNames = filterNames(names);
		write(builder, fNames, name);
	}

	private static Map<String, String> filterNames(Map<String, String> names) {
		return filterNames(names, new HashMap<String, String>());
	}

	private static Map<String, String> filterNames(Map<String, String> names, HashMap<String, String> filteredNames) {
		if(names == null) return filteredNames;

		if(names.get("name") != null) {
			filteredNames.put("default", names.get("name"));
		}

		for(String language : languages) {
			if(names.get("name:" + language) != null) {
				filteredNames.put(language, names.get("name:" + language));
			}
		}

		return filteredNames;
	}
}

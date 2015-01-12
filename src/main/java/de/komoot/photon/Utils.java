package de.komoot.photon;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.vividsolutions.jts.geom.Envelope;
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
	private static final Joiner commaJoiner = Joiner.on(", ").skipNulls();

	public static XContentBuilder convert(PhotonDoc doc, String[] languages) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
				.field(Constants.OSM_ID, doc.getOsmId())
				.field(Constants.OSM_TYPE, doc.getOsmType())
				.field(Constants.OSM_KEY, doc.getTagKey())
				.field(Constants.OSM_VALUE, doc.getTagValue())
				.field(Constants.IMPORTANCE, doc.getImportance());

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

		writeName(builder, doc.getName(), languages);
		writeIntlNames(builder, doc.getCity(), "city", languages);
		writeIntlNames(builder, doc.getCountry(), "country", languages);
		writeIntlNames(builder, doc.getState(), "state", languages);
		writeIntlNames(builder, doc.getStreet(), "street", languages);
		writeContext(builder, doc.getContext(), languages);
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

	private static void writeName(XContentBuilder builder, Map<String, String> name, String[] languages) throws IOException {
		Map<String, String> fNames = filterNames(name, languages);

		if(name.get("alt_name") != null)
			fNames.put("alt", name.get("alt_name"));

		if(name.get("int_name") != null)
			fNames.put("int", name.get("int_name"));

		if(name.get("loc_name") != null)
			fNames.put("loc", name.get("loc_name"));

		if(name.get("old_name") != null)
			fNames.put("old", name.get("old_name"));

		if(name.get("reg_name") != null)
			fNames.put("reg", name.get("reg_name"));

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

	protected static void writeContext(XContentBuilder builder, Set<Map<String, String>> contexts, String[] languages) throws IOException {
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

	private static void writeIntlNames(XContentBuilder builder, Map<String, String> names, String name, String[] languages) throws IOException {
		Map<String, String> fNames = filterNames(names, languages);
		write(builder, fNames, name);
	}

	private static Map<String, String> filterNames(Map<String, String> names, String[] languages) {
		return filterNames(names, new HashMap<String, String>(), languages);
	}

	private static Map<String, String> filterNames(Map<String, String> names, HashMap<String, String> filteredNames, String[] languages) {
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

	// http://stackoverflow.com/a/4031040/1437096
	public static String stripNonDigits(
			final CharSequence input /* inspired by seh's comment */) {
		final StringBuilder sb = new StringBuilder(
				input.length() /* also inspired by seh's comment */);
		for(int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if(c > 47 && c < 58) {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}

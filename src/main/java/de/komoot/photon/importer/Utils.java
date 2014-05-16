package de.komoot.photon.importer;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.komoot.photon.importer.model.PhotonDoc;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * date: 16.05.14
 *
 * @author christoph
 */
public class Utils {
	static final Joiner commaJoiner = Joiner.on(", ").skipNulls();

	final static String[] languages = new String[]{"de", "en", "fr", "it"};

	public static XContentBuilder convert(PhotonDoc doc) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
				.field("osm_id", doc.getOsmId())
				.field("osm_type", doc.getOsmType())

				.field("osm_key", doc.getTagKey())
				.field("osm_value", doc.getTagValue())

				.startObject("coordinate")
				.field("lat", doc.getCentroid().getY())
				.field("lon", doc.getCentroid().getX())
				.endObject();

		if(doc.getStreet() != null && doc.getStreet().get("name") != null) {
			builder.field("street", doc.getStreet().get("name"));
		}

		if(doc.getHouseNumber() != null) {
			builder.field("housenumber", doc.getHouseNumber());
		}

		if(doc.getPostcode() != null) {
			builder.field("postcode", doc.getPostcode());
		}

		writeIntlNames(builder, doc.getName(), "name");
		writeIntlNames(builder, doc.getCity(), "city");
		writeIntlNames(builder, doc.getContext(), "context");

		return builder;
	}

	private static void writeIntlNames(XContentBuilder builder, Set<Map<String, String>> contexts, String name) throws IOException {
		final SetMultimap<String, String> map = HashMultimap.create();

		for(Map<String, String> context : contexts) {
			if(context.get("name") != null) {
				map.put("default", context.get("name"));
			}
		}

		for(String language : languages) {
			for(Map<String, String> context : contexts) {
				if(context.get(language) != null) {
					map.put(language, context.get(language));
				}
			}
		}

		if(!map.isEmpty()) {
			builder.startObject(name);
			for(String key : map.keys()) {
				builder.field(key, commaJoiner.join(map.get(key)));
			}
			builder.endObject();
		}
	}

	private static void writeIntlNames(XContentBuilder builder, Map<String, String> map, String name) throws IOException {
		if(map == null || map.isEmpty()) return;

		builder.startObject(name);
		if(map.get("name") != null) {
			builder.field("default", map.get("name")); // TODO: consider also short_name, int_ref, ...
		}

		for(String language : languages) {
			if(map.get("name:" + language) != null) {
				builder.field(language, map.get("name:" + language));
			}
		}

		builder.endObject();
	}
}

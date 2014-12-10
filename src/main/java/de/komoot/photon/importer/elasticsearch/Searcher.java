package de.komoot.photon.importer.elasticsearch;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.komoot.photon.importer.Tags;
import de.komoot.photon.importer.osm.OSMTags;
import lombok.extern.slf4j.Slf4j;
import de.komoot.photon.importer.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * date: 24.05.14
 *
 * @author christoph
 */
@Slf4j
public class Searcher {
	private final String queryTemplate;
	private final String queryLocationBiasTemplate;
	private final Client client;

	/** These properties are directly copied into the result */
	private final static String[] KEYS_LANG_UNSPEC = {OSMTags.KEY_OSM_ID, OSMTags.KEY_OSM_VALUE, OSMTags.KEY_OSM_KEY, OSMTags.KEY_POSTCODE, OSMTags.KEY_HOUSENUMBER, OSMTags.KEY_OSM_TYPE};

	/** These properties will be translated before they are copied into the result */
	private final static String[] KEYS_LANG_SPEC = {OSMTags.KEY_NAME, OSMTags.KEY_COUNTRY, OSMTags.KEY_CITY, OSMTags.KEY_STREET};

	public Searcher(Client client) {
		this.client = client;
		try {
			final ClassLoader loader = Thread.currentThread().getContextClassLoader();
			queryTemplate = IOUtils.toString(loader.getResourceAsStream("query.json"), "UTF-8");
			queryLocationBiasTemplate = IOUtils.toString(loader.getResourceAsStream("query_location_bias.json"), "UTF-8");
		} catch(Exception e) {
			throw new RuntimeException("cannot access query templates", e);
		}
	}

	public List<JSONObject> search(String query, String lang, Double lon, Double lat, int limit, boolean matchAll) {
		final ImmutableMap.Builder<String, Object> params = ImmutableMap.<String, Object>builder()
				.put("query", StringEscapeUtils.escapeJson(query))
				.put("lang", lang)
				.put("should_match", matchAll ? "100%" : "-1");
		if(lon != null) params.put("lon", lon);
		if(lat != null) params.put("lat", lat);

		StrSubstitutor sub = new StrSubstitutor(params.build(), "${", "}");
		if(lon != null && lat != null) {
			query = sub.replace(queryLocationBiasTemplate);
		} else {
			query = sub.replace(queryTemplate);
		}

		SearchResponse response = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(query).setSize(limit).setTimeout(TimeValue.timeValueSeconds(7)).execute().actionGet();
		List<JSONObject> results = convert(response.getHits().getHits(), lang);
		results = removeStreetDuplicates(results, lang);
		if(results.size() > limit) {
			results = results.subList(0, limit);
		}
		return results;
	}

	private List<JSONObject> removeStreetDuplicates(List<JSONObject> results, String lang) {
		List<JSONObject> filteredItems = Lists.newArrayListWithCapacity(results.size());
		final HashSet<String> keys = Sets.newHashSet();
		for(JSONObject result : results) {
			final JSONObject properties = result.getJSONObject(Tags.KEY_PROPERTIES);
			if(properties.has(Tags.KEY_OSM_KEY) && "highway".equals(properties.getString(Tags.KEY_OSM_KEY))) {
				// result is a street
				if(properties.has(OSMTags.KEY_POSTCODE) && properties.has(OSMTags.KEY_NAME)) {
					// street has a postcode and name
					String postcode = properties.getString(OSMTags.KEY_POSTCODE);
					String name = properties.getString(OSMTags.KEY_NAME);
					String key = postcode + ":" + name;                                                                   

					if(keys.contains(key)) {
						// a street with this name + postcode is already part of the result list
						continue;
					} else if (lang.equals("nl")) {
                                                // check for dutch postcodes (i.e. 6532RA). If a street has the same name and the same 4 numbers in the postcode, 
                                                // we can assume it is part of the same street, so only use the first
                                                try {
                                                        String letterlessPostcode = Utils.stripNonDigits(postcode);
                                                        int postcodeNumbers = Integer.parseInt(letterlessPostcode);
                                                        boolean foundMatch = false;
                                                        
                                                        for (String keyString : keys) {
                                                                String letterlessKey = Utils.stripNonDigits(keyString);    
                                                                // also check if name equals, 
                                                                // which is a safety check for streets that partially match and have the same postcode numbers
                                                                String keyName = keyString.split(":")[1];
                                                                if (postcodeNumbers == Integer.parseInt(letterlessKey) && keyName.equals(name)) {
                                                                        foundMatch = true;
                                                                        break;
                                                                }
                                                        }
                                                        
                                                        if (foundMatch) {
                                                                continue;
                                                        }
                                                } catch (NumberFormatException e) {}                                                
                                        }
					keys.add(key);
				}
			}
			filteredItems.add(result);
		}

		return filteredItems;
	}

	private List<JSONObject> convert(SearchHit[] hits, final String lang) {
		final List<JSONObject> list = Lists.newArrayListWithExpectedSize(hits.length);
		for(SearchHit hit : hits) {
			final Map<String, Object> source = hit.getSource();

			final JSONObject feature = new JSONObject();
			feature.put(Tags.KEY_TYPE, Tags.VALUE_FEATURE);
			feature.put(Tags.KEY_GEOMETRY, getPoint(source));

			final JSONObject properties = new JSONObject();
			// language unspecific properties
			for(String key : KEYS_LANG_UNSPEC) {
				if(source.containsKey(key))
					properties.put(key, source.get(key));
			}

			// language specific properties
			for(String key : KEYS_LANG_SPEC) {
				if(source.containsKey(key))
					properties.put(key, getLocalised(source, key, lang));
			}

			// add extent of geometry
			final Map<String, Object> extent = (Map<String, Object>) source.get("extent");
			if(extent != null) {
				List<List<Double>> coords = (List<List<Double>>) extent.get("coordinates");
				final List<Double> nw = coords.get(0);
				final List<Double> se = coords.get(1);
				properties.put("extent", new JSONArray(Lists.newArrayList(nw.get(0), nw.get(1), se.get(0), se.get(1))));
			}

			feature.put(Tags.KEY_PROPERTIES, properties);
			list.add(feature);
		}
		return list;
	}

	private static JSONObject getPoint(Map<String, Object> source) {
		JSONObject point = new JSONObject();

		final Map<String, Double> coordinate = (Map<String, Double>) source.get("coordinate");
		if(coordinate != null) {
			point.put(Tags.KEY_TYPE, Tags.VALUE_POINT);
			point.put(Tags.KEY_COORDINATES, new JSONArray("[" + coordinate.get(Tags.KEY_LON) + "," + coordinate.get(Tags.KEY_LAT) + "]"));
		} else {
			log.error(String.format("invalid data [id=%s, type=%s], coordinate is missing!", source.get(OSMTags.KEY_OSM_ID), source.get(OSMTags.KEY_OSM_VALUE)));
		}

		return point;
	}

	private static String getLocalised(Map<String, Object> source, String fieldName, String lang) {
		final Map<String, String> map = (Map<String, String>) source.get(fieldName);
		if(map == null) return null;

		if(map.get(lang) != null) {
			// language specific field
			return map.get(lang);
		}

		return map.get("default");
	}
}

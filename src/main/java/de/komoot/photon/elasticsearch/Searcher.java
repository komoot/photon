package de.komoot.photon.elasticsearch;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.komoot.photon.Constants;
import de.komoot.photon.Utils;
import lombok.extern.slf4j.Slf4j;
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
@Deprecated
public class Searcher {
	private final String queryTemplate;
	private final String queryLocationBiasTemplate;
	private final String queryWithTagKeyValueFiltersTemplate;
	private final String queryWithTagKeyValueFiltersAndBiasTemplate;
	private final String queryWithTagKeyFiltersTemplate;
	private final String queryWithTagKeyFiltersAndBiasTemplate;
	private final Client client;

	/** These properties are directly copied into the result */
	private final static String[] KEYS_LANG_UNSPEC = {Constants.OSM_ID, Constants.OSM_VALUE, Constants.OSM_KEY, Constants.POSTCODE, Constants.HOUSENUMBER, Constants.OSM_TYPE};

	/** These properties will be translated before they are copied into the result */
	private final static String[] KEYS_LANG_SPEC = {Constants.NAME, Constants.COUNTRY, Constants.CITY, Constants.STREET, Constants.STATE};    

    public Searcher(Client client) {
		this.client = client;
		try {
			final ClassLoader loader = Thread.currentThread().getContextClassLoader();
			queryTemplate = IOUtils.toString(loader.getResourceAsStream("query.json"), "UTF-8");
			queryLocationBiasTemplate = IOUtils.toString(loader.getResourceAsStream("query_location_bias.json"), "UTF-8");
            queryWithTagKeyValueFiltersTemplate = IOUtils.toString(loader.getResourceAsStream("query_tag_key_value_filter.json"), "UTF-8");
            queryWithTagKeyValueFiltersAndBiasTemplate = IOUtils.toString(loader.getResourceAsStream("query_tag_key_value_filter_location_bias.json"), "UTF-8");
            queryWithTagKeyFiltersTemplate = IOUtils.toString(loader.getResourceAsStream("query_tag_key_filter.json"), "UTF-8");
            queryWithTagKeyFiltersAndBiasTemplate = IOUtils.toString(loader.getResourceAsStream("query_tag_key_filter_location_bias.json"), "UTF-8");
        } catch(Exception e) {
			throw new RuntimeException("cannot access query templates", e);
		}
	}

	public List<JSONObject> search(String query, String lang, Double lon, Double lat, String tagKey, String tagValue, int limit, boolean matchAll) {
		final ImmutableMap.Builder<String, Object> params = ImmutableMap.<String, Object>builder()
				.put("query", StringEscapeUtils.escapeJson(query))
				.put("lang", lang)
				.put("should_match", matchAll ? "100%" : "-1");
		if(lon != null) params.put("lon", lon);
		if(lat != null) params.put("lat", lat);

        boolean hasBias = lon!=null && lat!=null;
        boolean hasTagKey = tagKey!=null;
        boolean hasTagValue = tagValue!=null;
        boolean hasTagKeyNoValue = hasTagKey && !hasTagValue;
        boolean hasTagKeyHasValue = hasTagKey && hasTagValue;
        boolean hasTagKeyHasValueNoBias = hasTagKeyHasValue && !hasBias;
        boolean hasTagKeyHasValueHasBias = hasTagKeyHasValue && hasBias;
        boolean hasTagKeyNoValueNoBias = hasTagKeyNoValue && !hasBias;
        boolean hasTagKeyNoValueHasBias = hasTagKeyNoValue && hasBias;
        boolean hasNoTagKeyNoValueNoBias = !hasTagKey && !hasTagValue && !hasBias;
        boolean hasNoTagKeyNoValueHasBias =  !hasTagKey && !hasTagValue && hasBias;
        
        if (hasNoTagKeyNoValueHasBias) {
            StrSubstitutor sub = new StrSubstitutor(params.build(), "${", "}");
            query = sub.replace(queryLocationBiasTemplate);
        } else if (hasNoTagKeyNoValueNoBias) {
            StrSubstitutor sub = new StrSubstitutor(params.build(), "${", "}");
            query = sub.replace(queryTemplate);
        } else if (hasTagKeyNoValueHasBias) {
            params.put("osm_key",tagKey);
            StrSubstitutor sub = new StrSubstitutor(params.build(), "${", "}");
            query = sub.replace(queryWithTagKeyFiltersAndBiasTemplate);
        } else if (hasTagKeyNoValueNoBias) {
            params.put("osm_key",tagKey);
            StrSubstitutor sub = new StrSubstitutor(params.build(), "${", "}");
            query = sub.replace(queryWithTagKeyFiltersTemplate);
        } else if (hasTagKeyHasValueHasBias) {
            params.put("osm_key",tagKey);
            params.put("osm_value",tagValue);
            StrSubstitutor sub = new StrSubstitutor(params.build(), "${", "}");
            query = sub.replace(queryWithTagKeyValueFiltersAndBiasTemplate);
        } else if (hasTagKeyHasValueNoBias) {
            params.put("osm_key",tagKey);
            params.put("osm_value",tagValue);
            StrSubstitutor sub = new StrSubstitutor(params.build(), "${", "}");
            query = sub.replace(queryWithTagKeyValueFiltersTemplate);
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
			final JSONObject properties = result.getJSONObject(Constants.PROPERTIES);
			if(properties.has(Constants.OSM_KEY) && "highway".equals(properties.getString(Constants.OSM_KEY))) {
				// result is a street
				if(properties.has(Constants.POSTCODE) && properties.has(Constants.NAME)) {
					// street has a postcode and name
					String postcode = properties.getString(Constants.POSTCODE);
					String name = properties.getString(Constants.NAME);
					String key;

					if(lang.equals("nl")) {
						String onlyDigitsPostcode = Utils.stripNonDigits(postcode);
						key = onlyDigitsPostcode + ":" + name;
					} else {
						key = postcode + ":" + name;
					}

					if(keys.contains(key)) {
						// a street with this name + postcode is already part of the result list
						continue;
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
			feature.put(Constants.TYPE, Constants.FEATURE);
			feature.put(Constants.GEOMETRY, getPoint(source));

			// populate properties
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

			feature.put(Constants.PROPERTIES, properties);

			list.add(feature);
		}
		return list;
	}

	private static JSONObject getPoint(Map<String, Object> source) {
		JSONObject point = new JSONObject();

		final Map<String, Double> coordinate = (Map<String, Double>) source.get("coordinate");
		if(coordinate != null) {
			point.put(Constants.TYPE, Constants.POINT);
			point.put(Constants.COORDINATES, new JSONArray("[" + coordinate.get(Constants.LON) + "," + coordinate.get(Constants.LAT) + "]"));
		} else {
			log.error(String.format("invalid data [id=%s, type=%s], coordinate is missing!", source.get(Constants.OSM_ID), source.get(Constants.OSM_VALUE)));
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

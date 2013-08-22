package de.komoot.search.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import de.komoot.search.model.SolrDocument;
import de.komoot.search.utils.Constants.COLUMNS;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author christoph
 */
public class ResultEvaluator {
	public static final double MINIMUM_SIMILARITY_NAME = 0.8;
	public static final double MIN_FIELD_SIMILARITY = 0.9;

	private static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}

	/**
	 * http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/ Levenshtein_distance#Java
	 *
	 * @param str1
	 * @param str2
	 * @return
	 */
	public static int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
		int[][] distance = new int[str1.length() + 1][str2.length() + 1];

		for(int i = 0; i <= str1.length(); i++)
			distance[i][0] = i;
		for(int j = 1; j <= str2.length(); j++)
			distance[0][j] = j;

		for(int i = 1; i <= str1.length(); i++)
			for(int j = 1; j <= str2.length(); j++)
				distance[i][j] = minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1,
						distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));

		return distance[str1.length()][str2.length()];
	}

	/**
	 * validates results
	 *
	 * @param results
	 * @param line
	 * @return true if at least one result was the one expected
	 */
	public static boolean hasSuccessfulResult(List<SolrDocument> results, CSVLine line) {
		List<Map<COLUMNS, Boolean>> matrix = generateReports(results, line);

		for(Map<COLUMNS, Boolean> testResult : matrix) {
			int testFailCount = 0;
			for(boolean b : testResult.values()) {
				if(b == false) testFailCount++;
			}
			if(testFailCount == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * generates a detailed report for each result
	 *
	 * @param results search results
	 * @param line    test case
	 * @return list of maps. the key of the map is the column, the value boolean (true = result complies with test case,
	 *         false = failure)
	 */
	public static List<Map<COLUMNS, Boolean>> generateReports(List<SolrDocument> results, CSVLine line) {
		List<Map<COLUMNS, Boolean>> reports = Lists.newArrayList();

		for(int index = 0; index < results.size(); index++) {
			SolrDocument result = results.get(index);

			Map<COLUMNS, Boolean> report = Maps.newHashMap();

			for(String colKey : line.getColumnKeys()) {
				COLUMNS column = line.get(colKey);
				String expectedValue = line.get(column);
				double allowedSimilarity = MIN_FIELD_SIMILARITY;

				String actualValue = null;
				switch(column) {
					case CITY:
						actualValue = result.getCity().getValue(Locale.getDefault());
						break;
					case COUNTRY:
						actualValue = result.getCountry().getValue(Locale.ENGLISH);
						break;
					case STREET:
						actualValue = result.getStreet();
						break;
					case HOUSENUMBER:
						actualValue = result.getHousenumber();
						break;
					case ZIP:
						actualValue = result.getPostcode();
						break;
					case COORDINATE:
						if(false == expectedValue.equals(COLUMNS.COORDINATE.getDefaultValue()) && getCoordinate(result) != null) {
							Double maxDistance = Double.parseDouble(line.get(COLUMNS.DISTANCE_TOLERANCE));
							double distance = distance(line.getCoordinate(), getCoordinate(result));
							report.put(column, distance < maxDistance);
						}
						continue;
					case MAX_RESULT_INDEX:
						report.put(column, index <= Integer.parseInt(expectedValue));
						continue;
					case NAME:
						allowedSimilarity = MINIMUM_SIMILARITY_NAME;
						actualValue = result.getName().getValue(Locale.ENGLISH);
						break;
				}

				if(hasText(expectedValue)) {
					boolean success = getSimilarity(expectedValue, actualValue) > allowedSimilarity;
					report.put(column, success);
				}
			}
			reports.add(report);
		}

		return reports;
	}

	private static Coordinate getCoordinate(SolrDocument result) {
		String[] yx = result.getCoordinate().split(",");
		return new Coordinate(Double.parseDouble(yx[0]), Double.parseDouble(yx[1]));
	}

	/**
	 * returns the similarity between two string expressed in value between 0 (no similarity) and 1 (equal)
	 *
	 * @param name0
	 * @param name1
	 * @return
	 */

	public static double getSimilarity(String name0, String name1) {
		if(name0 == null || name1 == null) {
			return 0;
		}

		name0 = name0.trim().toLowerCase();
		name1 = name1.trim().toLowerCase();

		if(name0.equals(name1)) {
			return 1;
		}

		int levDistance = ResultEvaluator.computeLevenshteinDistance(name0, name1);

		int denominator = Math.max(name0.length(), name1.length());

		if(denominator == 0) {
			return 0;
		}
		return 1 - ((double) levDistance) / denominator;
	}

	public static double distance(Coordinate c0, Coordinate c1) {
		double dLon = (c1.x - c0.y) * (Math.PI / 180d);
		double dLat = (c0.y - c1.y) * (Math.PI / 180d);

		double a = Math.pow(Math.sin(dLat / 2D), 2D) + Math.cos(c0.y * (Math.PI / 180D)) *
				Math.cos(c1.y * (Math.PI / 180D)) * Math.pow(Math.sin(dLon / 2D), 2D);
		double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));

		return 6378137.0 * c;
	}
}

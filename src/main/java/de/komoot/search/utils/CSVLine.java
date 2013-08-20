package de.komoot.search.utils;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.commons.lang.LocaleUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CSVLine implements Constants {
	private final Map<String, String> columns;

	public CSVLine(Map<String, String> columns) {
		this.columns = columns;
		assertValid(columns);
	}

	/**
	 * returns the value of csv line, if not defined get column default value
	 *
	 * @param columns
	 * @return
	 */
	public String get(COLUMNS columns) {
		String value = this.columns.get(columns.getId());

		if(value == null || false == StringUtils.hasText(value)) {
			return columns.getDefaultValue();
		}

		return value;
	}

	public COLUMNS get(String columnKey) {
		return COLUMNS.valueOf(columnKey.toUpperCase());
	}

	public Set<String> getColumnKeys() {
		return this.columns.keySet();
	}

	/**
	 * assures that test case is designed as expected
	 *
	 * @param columns
	 */
	private void assertValid(Map<String, String> columns) {
		// assures that columns keys are the once expected
		Set<String> rowNames = columns.keySet();
		for(String rowName : rowNames) {
			if(false == columns.keySet().contains(rowName)) {
				throw new RuntimeException(String.format("invalid csv file, unexpected row name [%s]", rowName));
			}
		}
	}

	public static Locale getLocale(String column) {
		return LocaleUtils.toLocale(column);
	}

	public Coordinate getCoordinate() {
		String[] splinter = get(COLUMNS.COORDINATE).split(",");

		Assert.isTrue(splinter.length > 1, "cannot convert column [" + get(COLUMNS.COORDINATE) + "] to coordinate");

		Double x = Double.valueOf(splinter[0]);
		Double y = Double.valueOf(splinter[1]);

		Assert.notNull(x);
		Assert.notNull(y);

		return new Coordinate(x, y);
	}

	@Override
	public String toString() {
		return this.columns.entrySet().toString();
	}
}

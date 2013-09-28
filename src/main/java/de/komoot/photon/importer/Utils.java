package de.komoot.photon.importer;

import com.google.common.collect.ImmutableMap;
import de.komoot.photon.importer.model.I18nName;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * static utility class
 * date: 28.09.13
 *
 * @author christoph
 */
public class Utils {
	/**
	 * @param resultSet
	 * @param columnName
	 * @param languages
	 * @return
	 * @throws SQLException
	 */
	public static I18nName createI18nName(ResultSet resultSet, String columnName, List<String> languages) throws SQLException {
		String name = resultSet.getString(columnName);

		ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
		for(String language : languages) {
			String translation = resultSet.getString(columnName + "_" + language);
			if(StringUtils.hasText(translation)) {
				builder.put(language, translation);
			}
		}

		return new I18nName(name, builder.build());
	}
}

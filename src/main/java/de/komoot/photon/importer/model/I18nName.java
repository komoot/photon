package de.komoot.photon.importer.model;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * stores name in different languages including locale name
 *
 * @author christoph
 */
public class I18nName {
	public static final I18nName LONDON = new I18nName("London", "London", "London", "Londres", "Londra");
	private static final Map<String, String> emptyTranslations = Collections.emptyMap();

	private final String name;
	private final Map<String, String> translations;

	/** constructor for a nameless entry */
	public I18nName() {
		this.name = null;
		this.translations = emptyTranslations;
	}

	/**
	 * constructor for names without translations
	 *
	 * @param name
	 */
	public I18nName(String name) {
		this.name = name;
		this.translations = emptyTranslations;
	}

	/**
	 * constructor for names with translations
	 *
	 * @param name
	 * @param translations
	 */
	public I18nName(String name, Map<String, String> translations) {
		this.name = name;
		this.translations = translations;
	}

	@Deprecated
	public I18nName(String name, String de, String en, String fr, String it) {
		this.name = name;
		ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
		if(de != null) builder.put("de", de);
		if(en != null) builder.put("en", en);
		if(fr != null) builder.put("fr", fr);
		if(it != null) builder.put("it", it);
		this.translations = builder.build();
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getTranslations() {
		return translations;
	}

	@Override
	public String toString() {
		return "I18nName{" +
				"name='" + name + '\'' +
				", translations=" + translations +
				"} ";
	}

	/**
	 * returns the name in given language, if no language specific name exists, local name will be returned
	 *
	 * @param language can be null, will return local value
	 * @return
	 */
	@Nullable
	public String get(@Nullable String language) {
		if(language == null) {
			return name;
		}

		if(translations.containsKey(language)) {
			return translations.get(language);
		}

		return name;
	}

	/**
	 * check if at least one name (in any language) was set
	 *
	 * @return
	 */
	public boolean isNameless() {
		return StringUtils.hasText(name) == false && translations.isEmpty();
	}
}

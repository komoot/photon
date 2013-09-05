package de.komoot.photon.importer.model;

import org.springframework.util.StringUtils;

/**
 * stores name in different languages including locale default
 *
 * @author christoph
 */
public class I18nName {
	public static final I18nName LONDON = new I18nName("London", "London", "London", "Londres", "Londra");

	public final String de;
	public final String en;
	public final String fr;
	public final String it;
	public final String locale;

	public I18nName(String locale, String de, String en, String fr, String it) {
		this.locale = locale;
		this.de = de;
		this.en = en;
		this.fr = fr;
		this.it = it;
	}

	/** constructor for a nameless entry */
	public I18nName() {
		this.locale = null;
		this.de = null;
		this.en = null;
		this.fr = null;
		this.it = null;
	}

	/**
	 * check if at least one name was set
	 *
	 * @return
	 */
	public boolean isNameless() {
		if(StringUtils.hasText(locale)) return false;
		if(StringUtils.hasText(de)) return false;
		if(StringUtils.hasText(en)) return false;
		if(StringUtils.hasText(fr)) return false;
		if(StringUtils.hasText(it)) return false;

		return true;
	}

	@Override
	public String toString() {
		return String.format("I18nName locale: %s, de: %s, en: %s, fr: %s, it: %s", locale, de, en, fr, it);
	}
}

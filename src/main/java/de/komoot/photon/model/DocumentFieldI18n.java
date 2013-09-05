package de.komoot.photon.model;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * model that represents a multilingual field
 */
public class DocumentFieldI18n {
	private String localeValue;
	private String valueDe;
	private String valueEn;
	private String valueFr;
	private String valueIt;

	public DocumentFieldI18n(String localeValue, String valueDe, String valueEn, String valueFr, String valueIt) {
		this.localeValue = localeValue;
		this.valueDe = valueDe;
		this.valueEn = valueEn;
		this.valueFr = valueFr;
		this.valueIt = valueIt;
	}

	public String getValue(Locale locale) {
		if(Locale.GERMAN.getLanguage().equals(locale.getLanguage()) && isValid(valueDe)) {
			return valueDe;
		}

		if(Locale.ENGLISH.getLanguage().equals(locale.getLanguage()) && isValid(valueEn)) {
			return valueEn;
		}

		if(Locale.FRENCH.getLanguage().equals(locale.getLanguage()) && isValid(valueFr)) {
			return valueFr;
		}

		if(Locale.ITALIAN.getLanguage().equals(locale.getLanguage()) && isValid(valueIt)) {
			return valueIt;
		}

		return localeValue;
	}

	private boolean isValid(String value) {
		return StringUtils.hasText(value);
	}
}

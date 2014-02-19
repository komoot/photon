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
	private String valueEs;
        private String valuePt;
	private String valueJa;
	private String valueZh;

    public DocumentFieldI18n(String localeValue, String valueDe, String valueEn, String valueFr, String valueIt, String valueEs, String valuePt, String valueJa, String valueZh) {
		this.localeValue = localeValue;
		this.valueDe = valueDe;
		this.valueEn = valueEn;
		this.valueFr = valueFr;
		this.valueIt = valueIt;
		this.valueEs = valueEs;
		this.valuePt = valuePt;
		this.valueJa = valueJa;
		this.valueZh = valueZh;
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

                if(locale.getLanguage().equals(new Locale("es", "", "")) && isValid(valueEs)) {
			return valueEs;
		}

                if(locale.getLanguage().equals(new Locale("pt", "", "")) && isValid(valuePt)) {
			return valuePt;
		}

		if(Locale.JAPANESE.getLanguage().equals(locale.getLanguage()) && isValid(valueJa)) {
			return valueJa;
		}

		if(Locale.TRADITIONAL_CHINESE.getLanguage().equals(locale.getLanguage()) && isValid(valueZh)) {
			return valueZh;
		}

		return localeValue;
	}

	private boolean isValid(String value) {
		return StringUtils.hasText(value);
	}
}

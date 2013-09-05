package de.komoot.photon.importer;

import de.komoot.photon.importer.model.I18nName;

/**
 * User: christoph Date: 29.07.13
 */
public class Constants {
	/**
	 * states
	 */
	public final I18nName VORARLBERG = new I18nName("Vorarlberg", "Vorarlberg", "Vorarlberg", "Vorarlberg", "Vorarlberg");
	public final I18nName ENGLAND = new I18nName("England", "England", "England", null, null);

	public final I18nName BERLIN = new I18nName("Berlin", "Berlin", "Berlin", "Berlin", "Berlino");
	public final I18nName POTSDAM = new I18nName("Potsdam", "Potsdam", "Potsdam", "Potsdam", "Potsdam");
	public final I18nName LONDON = new I18nName("London", "London", "London", "Londres", "Londra");

	public final I18nName BEZIRK_BREGENZ = new I18nName("Bezirk Bregenz", "Bezirk Bregenz", null, null, null);
	public final I18nName MITTE = new I18nName("Mitte", "Mitte", null, null, null);

	public final I18nName NULL_NAME = new I18nName();

	protected I18nName name(String locale) {
		return new I18nName(locale, null, null, null, null);
	}
}

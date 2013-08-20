package de.komoot.search.importer.model;

/**
 * model to store a nation's admin levels for city and country
 *
 * @author christoph
 */
public class AdminScheme {
	final public int country;
	final public int city;

	public AdminScheme(int country, int city) {
		this.country = country;
		this.city = city;
	}
}

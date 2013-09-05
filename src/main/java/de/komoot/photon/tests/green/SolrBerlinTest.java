package de.komoot.photon.tests.green;

import de.komoot.photon.SolrTestTemplate;

/*
 * Auf www.komoot.de in Position 1 gefundener Eintrag (osm_id:2050132586) ist label der relation berlin; ranking=23.
 * Node (osm_id:240109189) sollte aber gefunden werden (Friedrichstraße); ranking=15.
 * --> Hohe distance tolerance in csv lässt test erfolgreich durchlaufen.
 */
public class SolrBerlinTest extends SolrTestTemplate {
	public SolrBerlinTest() {
		super("berlin");
	}
}
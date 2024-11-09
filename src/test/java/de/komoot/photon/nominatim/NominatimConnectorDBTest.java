package de.komoot.photon.nominatim;

import org.locationtech.jts.io.ParseException;
import de.komoot.photon.AssertUtil;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.testdb.CollectingImporter;
import de.komoot.photon.nominatim.testdb.H2DataAdapter;
import de.komoot.photon.nominatim.testdb.OsmlineTestRow;
import de.komoot.photon.nominatim.testdb.PlacexTestRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;

class NominatimConnectorDBTest {
    private EmbeddedDatabase db;
    private NominatimImporter connector;
    private CollectingImporter importer;
    private JdbcTemplate jdbc;
    private TransactionTemplate txTemplate;

    @BeforeEach
    void setup() {
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("/test-schema.sql")
                .build();


        connector = new NominatimImporter(null, 0, null, null, null, new H2DataAdapter());
        importer = new CollectingImporter();

        jdbc = new JdbcTemplate(db);
        txTemplate = new TransactionTemplate(new DataSourceTransactionManager(db));
        ReflectionTestUtil.setFieldValue(connector, NominatimConnector.class, "template", jdbc);
        ReflectionTestUtil.setFieldValue(connector, NominatimConnector.class, "txTemplate", txTemplate);
    }

    private void readEntireDatabase() {
        ImportThread importThread = new ImportThread(importer);
        try {
            for (var country: connector.getCountriesFromDatabase()) {
                connector.readCountry(country, importThread);
            }
        } finally {
            importThread.finish();
        }

    }

    @Test
    void testSimpleNodeImport() throws ParseException {
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("Spot").add(jdbc);
        readEntireDatabase();

        assertEquals(1, importer.size());
        importer.assertContains(place);
    }

    @Test
    void testImportForSelectedCountries() throws ParseException {
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("SpotHU").country("hu").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotDE").country("de").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotUS").country("us").add(jdbc);

        ImportThread importThread = new ImportThread(importer);
        try {
            connector.readCountry("uk", importThread);
            connector.readCountry("hu", importThread);
            connector.readCountry("nl", importThread);
        } finally {
            importThread.finish();
        }

        assertEquals(1, importer.size());
        importer.assertContains(place);
    }

    @Test
    void testImportance() {
        PlacexTestRow place1 = new PlacexTestRow("amenity", "cafe").name("Spot").rankSearch(10).add(jdbc);
        PlacexTestRow place2 = new PlacexTestRow("amenity", "cafe").name("Spot").importance(0.3).add(jdbc);

        readEntireDatabase();

        assertEquals(0.5, importer.get(place1.getPlaceId()).getImportance(), 0.00001);
        assertEquals(0.3, importer.get(place2.getPlaceId()).getImportance(), 0.00001);
    }

    @Test
    void testPlaceAddress() throws ParseException {
        PlacexTestRow place = PlacexTestRow.make_street("Burg").add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "neighbourhood").name("Le Coin").ranks(24).add(jdbc),
                new PlacexTestRow("place", "suburb").name("Crampton").ranks(20).add(jdbc),
                new PlacexTestRow("place", "city").name("Grand Junction").ranks(16).add(jdbc),
                new PlacexTestRow("place", "county").name("Lost County").ranks(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").ranks(8).add(jdbc));

        readEntireDatabase();

        assertEquals(6, importer.size());
        importer.assertContains(place);

        PhotonDoc doc = importer.get(place);

        AssertUtil.assertAddressName("Le Coin", doc, AddressType.LOCALITY);
        AssertUtil.assertAddressName("Crampton", doc, AddressType.DISTRICT);
        AssertUtil.assertAddressName("Grand Junction", doc, AddressType.CITY);
        AssertUtil.assertAddressName("Lost County", doc, AddressType.COUNTY);
        AssertUtil.assertAddressName("Le Havre", doc, AddressType.STATE);
    }

    @Test
    void testPlaceAddressAddressRank0() throws ParseException {
        PlacexTestRow place = new PlacexTestRow("natural", "water").name("Lake Tee").rankAddress(0).rankSearch(20).add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "county").name("Lost County").ranks(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").ranks(8).add(jdbc));

        readEntireDatabase();

        assertEquals(3, importer.size());
        importer.assertContains(place);

        PhotonDoc doc = importer.get(place);

        AssertUtil.assertAddressName("Lost County", doc, AddressType.COUNTY);
        AssertUtil.assertAddressName("Le Havre", doc, AddressType.STATE);
    }

    @Test
    void testPoiAddress() throws ParseException {
        PlacexTestRow parent = PlacexTestRow.make_street("Burg").add(jdbc);

        parent.addAddresslines(jdbc,
                new PlacexTestRow("place", "city").name("Grand Junction").ranks(16).add(jdbc));

        PlacexTestRow place = new PlacexTestRow("place", "house").name("House").parent(parent).add(jdbc);

        readEntireDatabase();

        assertEquals(3, importer.size());
        importer.assertContains(place);

        PhotonDoc doc = importer.get(place);

        AssertUtil.assertAddressName("Burg", doc, AddressType.STREET);
        AssertUtil.assertNoAddress(doc, AddressType.LOCALITY);
        AssertUtil.assertNoAddress(doc, AddressType.DISTRICT);
        AssertUtil.assertAddressName("Grand Junction", doc, AddressType.CITY);
        AssertUtil.assertNoAddress(doc, AddressType.COUNTY);
        AssertUtil.assertNoAddress(doc, AddressType.STATE);
    }

    @Test
    void testInterpolationPoint() throws ParseException {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(45, 45, 1).parent(street).geom("POINT(45 23)").add(jdbc);

        readEntireDatabase();

        assertEquals(2, importer.size());

        PlacexTestRow expect = new PlacexTestRow("place", "house_number")
                .id(osmline.getPlaceId())
                .parent(street)
                .osm("W", 23)
                .centroid(45.0, 23.0);

        importer.assertContains(expect, "45");
    }

    @Test
    void testInterpolationAny() throws ParseException {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(1, 11, 1).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);

        readEntireDatabase();

        assertEquals(12, importer.size());

        PlacexTestRow expect = new PlacexTestRow("place", "house_number").id(osmline.getPlaceId()).parent(street).osm("W", 23);

        for (int i = 1; i <= 11; ++i) {
            importer.assertContains(expect.centroid(0, (i - 1) * 0.1), String.valueOf(i));
        }
    }

    @Test
    void testInterpolationWithSteps() throws ParseException {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(10, 20, 2).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);

        readEntireDatabase();

        assertEquals(7, importer.size());

        PlacexTestRow expect = new PlacexTestRow("place", "house_number").id(osmline.getPlaceId()).parent(street).osm("W", 23);

        for (int i = 0; i < 6; ++i) {
            importer.assertContains(expect.centroid(0, i * 0.2), String.valueOf(10 + i * 2));
        }
    }

    /**
     * When the address contains multiple address parts that map to 'city', then only the one with the
     * highest address rank is used. The others are moved to context.
     */
    @Test
    void testAddressMappingDuplicate() {
        PlacexTestRow place = PlacexTestRow.make_street("Main Street").add(jdbc);
        PlacexTestRow munip = new PlacexTestRow("place", "municipality").name("Gemeinde").ranks(14).add(jdbc);

        place.addAddresslines(jdbc,
                munip,
                new PlacexTestRow("place", "village").name("Dorf").ranks(16).add(jdbc));

        readEntireDatabase();

        assertEquals(3, importer.size());

        PhotonDoc doc = importer.get(place);

        AssertUtil.assertAddressName("Dorf", doc, AddressType.CITY);
        assertTrue(doc.getContext().contains(munip.getNames()));
    }

    /**
     * When a city item has address parts that map to 'city' then these parts are moved to context.
     */
    @Test
    void testAddressMappingAvoidSameTypeAsPlace() {
        PlacexTestRow village = new PlacexTestRow("place", "village").name("Dorf").ranks(16).add(jdbc);
        PlacexTestRow munip = new PlacexTestRow("place", "municipality").name("Gemeinde").ranks(14).add(jdbc);

        village.addAddresslines(jdbc, munip);

        readEntireDatabase();

        assertEquals(2, importer.size());

        PhotonDoc doc = importer.get(village);

        AssertUtil.assertNoAddress(doc, AddressType.CITY);
        assertTrue(doc.getContext().contains(munip.getNames()));
    }

    /**
     * Unnamed objects are imported when they have a housenumber.
     */
    @Test
    void testUnnamedObjectWithHousenumber() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes").addr("housenumber", "123").parent(parent).add(jdbc);

        readEntireDatabase();

        assertEquals(2, importer.size());

        PhotonDoc doc = importer.get(place);
        assertEquals("123", doc.getHouseNumber());
    }

    /**
     * Semicolon-separated housenumber lists result in multiple iport objects.
     */
    @Test
    void testObjectWithHousenumberList() throws ParseException {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes").addr("housenumber", "1;2a;3").parent(parent).add(jdbc);

        readEntireDatabase();

        assertEquals(4, importer.size());

        importer.assertContains(place, "1");
        importer.assertContains(place, "2a");
        importer.assertContains(place, "3");
    }

    /**
     * streetnumbers and conscription numbers are recognised.
     */
    @Test
    void testObjectWithconscriptionNumber() throws ParseException {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes")
                .addr("streetnumber", "34")
                .addr("conscriptionnumber", "99521")
                .parent(parent).add(jdbc);

        readEntireDatabase();

        assertEquals(3, importer.size());

        importer.assertContains(place, "34");
        importer.assertContains(place, "99521");
    }
    /**
     * Unnamed objects are ignored when they do not have a housenumber.
     */
    @Test
    void testUnnamedObjectWithOutHousenumber() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        new PlacexTestRow("building", "yes").parent(parent).add(jdbc);

        readEntireDatabase();

        assertEquals(1, importer.size());

        importer.get(parent);
    }

    /**
     * Interpolation lines in placex are ignored.
     */
    @Test
    void testInterpolationLines() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        new PlacexTestRow("place", "houses").name("something").parent(parent).add(jdbc);

        readEntireDatabase();

        assertEquals(1, importer.size());

        importer.get(parent);
    }

    /**
     * Places without a country can be imported.
     */
    @Test
    void testNoCountry() {
        PlacexTestRow place = new PlacexTestRow("building", "yes").name("Building").country(null).add(jdbc);

        readEntireDatabase();

        assertEquals(1, importer.size());

        assertNull(importer.get(place).getCountryCode());
    }

    @Test
    void testGetImportDate() {
        Date importDate = connector.getLastImportDate();
        assertNull(importDate);
        
        importDate = new Date();
        jdbc.update("INSERT INTO import_status(lastimportdate, indexed) VALUES(?, ?)", importDate, true);
        assertEquals(importDate, connector.getLastImportDate());
    }
}
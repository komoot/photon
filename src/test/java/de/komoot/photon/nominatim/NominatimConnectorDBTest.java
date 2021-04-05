package de.komoot.photon.nominatim;

import com.vividsolutions.jts.io.ParseException;
import de.komoot.photon.AssertUtil;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.testdb.CollectingImporter;
import de.komoot.photon.nominatim.testdb.H2DataAdapter;
import de.komoot.photon.nominatim.testdb.OsmlineTestRow;
import de.komoot.photon.nominatim.testdb.PlacexTestRow;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;


public class NominatimConnectorDBTest {
    private EmbeddedDatabase db;
    private NominatimConnector connector;
    private CollectingImporter importer;
    private JdbcTemplate jdbc;

    @Before
    public void setup() {
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("/test-schema.sql")
                .build();


        connector = new NominatimConnector(null, 0, null, null, null, new H2DataAdapter());
        importer = new CollectingImporter();
        connector.setImporter(importer);

        jdbc = new JdbcTemplate(db);
        ReflectionTestUtil.setFieldValue(connector, "template", jdbc);
    }

    @Test
    public void testSimpleNodeImport() throws ParseException {
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("Spot").add(jdbc);
        connector.readEntireDatabase();

        Assert.assertEquals(1, importer.size());
        importer.assertContains(place);
    }

    @Test
    public void testImportForSelectedCountries() throws ParseException {
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("SpotHU").country("hu").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotDE").country("de").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotUS").country("us").add(jdbc);
        connector.readEntireDatabase("uk", "hu", "nl");

        Assert.assertEquals(1, importer.size());
        importer.assertContains(place);
    }

    @Test
    public void testImportance() {
        PlacexTestRow place1 = new PlacexTestRow("amenity", "cafe").name("Spot").rankSearch(10).add(jdbc);
        PlacexTestRow place2 = new PlacexTestRow("amenity", "cafe").name("Spot").importance(0.3).add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(0.5, importer.get(place1.getPlaceId()).getImportance(), 0.00001);
        Assert.assertEquals(0.3, importer.get(place2.getPlaceId()).getImportance(), 0.00001);
    }

    @Test
    public void testPlaceAddress() throws ParseException {
        PlacexTestRow place = PlacexTestRow.make_street("Burg").add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "neighbourhood").name("Le Coin").rankAddress(24).add(jdbc),
                new PlacexTestRow("place", "suburb").name("Crampton").rankAddress(20).add(jdbc),
                new PlacexTestRow("place", "city").name("Grand Junction").rankAddress(16).add(jdbc),
                new PlacexTestRow("place", "county").name("Lost County").rankAddress(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").rankAddress(8).add(jdbc));

        connector.readEntireDatabase();

        Assert.assertEquals(6, importer.size());
        importer.assertContains(place);

        PhotonDoc doc = importer.get(place);

        AssertUtil.assertAddressName("Le Coin", doc, AddressType.LOCALITY);
        AssertUtil.assertAddressName("Crampton", doc, AddressType.DISTRICT);
        AssertUtil.assertAddressName("Grand Junction", doc, AddressType.CITY);
        AssertUtil.assertAddressName("Lost County", doc, AddressType.COUNTY);
        AssertUtil.assertAddressName("Le Havre", doc, AddressType.STATE);
    }

    @Test
    public void testPoiAddress() throws ParseException {
        PlacexTestRow parent = PlacexTestRow.make_street("Burg").add(jdbc);

        parent.addAddresslines(jdbc,
                new PlacexTestRow("place", "city").name("Grand Junction").rankAddress(16).add(jdbc));

        PlacexTestRow place = new PlacexTestRow("place", "house").name("House").parent(parent).add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(3, importer.size());
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
    public void testInterpolationAny() throws ParseException {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(1, 11, "all").parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(10, importer.size());

        PlacexTestRow expect = new PlacexTestRow("place", "house_number").id(osmline.getPlaceId()).parent(street).osm("W", 23);

        for (int i = 2; i < 11; ++i) {
            importer.assertContains(expect.centroid(0, (i - 1) * 0.1), i);
        }
    }

    /**
     * When the address contains multiple address parts that map to 'city', then only the one with the
     * highest address rank is used. The others are moved to context.
     */
    @Test
    public void testAddressMappingDuplicate() {
        PlacexTestRow place = PlacexTestRow.make_street("Main Street").add(jdbc);
        PlacexTestRow munip = new PlacexTestRow("place", "municipality").name("Gemeinde").rankAddress(14).add(jdbc);

        place.addAddresslines(jdbc,
                munip,
                new PlacexTestRow("place", "village").name("Dorf").rankAddress(16).add(jdbc));

        connector.readEntireDatabase();

        Assert.assertEquals(3, importer.size());

        PhotonDoc doc = importer.get(place);

        AssertUtil.assertAddressName("Dorf", doc, AddressType.CITY);
        Assert.assertTrue(doc.getContext().contains(munip.getNames()));
    }

    /**
     * When a city item has address parts that map to 'city' then these parts are moved to context.
     */
    @Test
    public void testAddressMappingAvoidSameTypeAsPlace() {
        PlacexTestRow village = new PlacexTestRow("place", "village").name("Dorf").rankAddress(16).add(jdbc);
        PlacexTestRow munip = new PlacexTestRow("place", "municipality").name("Gemeinde").rankAddress(14).add(jdbc);

        village.addAddresslines(jdbc, munip);

        connector.readEntireDatabase();

        Assert.assertEquals(2, importer.size());

        PhotonDoc doc = importer.get(village);

        AssertUtil.assertNoAddress(doc, AddressType.CITY);
        Assert.assertTrue(doc.getContext().contains(munip.getNames()));
    }

    /**
     * Unnamed objects are imported when they have a housenumber.
     */
    @Test
    public void testUnnamedObjectWithHousenumber() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes").addr("housenumber", "123").parent(parent).add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(2, importer.size());

        PhotonDoc doc = importer.get(place);
        Assert.assertEquals(doc.getHouseNumber(), "123");
    }

    /**
     * Semicolon-separated housenumber lists result in multiple iport objects.
     */
    @Test
    public void testObjectWithHousenumberList() throws ParseException {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes").addr("housenumber", "1;2;3").parent(parent).add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(4, importer.size());

        importer.assertContains(place, 1);
        importer.assertContains(place, 2);
        importer.assertContains(place, 3);
    }

    /**
     * streetnumbers and conscription numbers are recognised.
     */
    @Test
    public void testObjectWithconscriptionNumber() throws ParseException {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes")
                .addr("streetnumber", "34")
                .addr("conscriptionnumber", "99521")
                .parent(parent).add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(3, importer.size());

        importer.assertContains(place, 34);
        importer.assertContains(place, 99521);
    }
    /**
     * Unnamed objects are ignored when they do not have a housenumber.
     */
    @Test
    public void testUnnamedObjectWithOutHousenumber() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes").parent(parent).add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(1, importer.size());

        importer.get(parent);
    }

    /**
     * Interpolation lines in placex are ignored.
     */
    @Test
    public void testInterpolationLines() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("place", "houses").name("something").parent(parent).add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(1, importer.size());

        importer.get(parent);
    }
}
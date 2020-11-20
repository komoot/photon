package de.komoot.photon.nominatim;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.testdb.CollectingImporter;
import de.komoot.photon.nominatim.testdb.OsmlineTestRow;
import de.komoot.photon.nominatim.testdb.PlacexTestRow;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


@Slf4j
public class NominatimConnectorDBTest {
    private AutoCloseable closeable;

    DBUtils h2dbutils;

    private EmbeddedDatabase db;
    private NominatimConnector connector;
    private CollectingImporter importer;
    private JdbcTemplate jdbc;


    @Before
    public void setup() throws NoSuchMethodException, SQLException {
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("/test-schema.sql")
                .build();



        connector = new NominatimConnector(null, 0, null, null, null);
        importer = new CollectingImporter();
        connector.setImporter(importer);

        h2dbutils = Mockito.mock(DBUtils.class);

        Mockito.when(h2dbutils.extractGeometry(Mockito.any(), Mockito.anyString())).thenAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws SQLException {
                Object[] args = invocation.getArguments();
                ResultSet rs = (ResultSet) args[0];
                String column = (String) args[1];

                String wkt = (String) rs.getObject(column);
                if (wkt != null) {
                    try {
                        return new WKTReader().read(wkt);
                    } catch (ParseException e) {
                        // ignore
                    }
                }

                return null;
            }
        });

        Mockito.when(h2dbutils.getMap(Mockito.any(), Mockito.anyString())).thenAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws SQLException {
                Object[] args = invocation.getArguments();
                ResultSet rs = (ResultSet) args[0];
                String column = (String) args[1];

                Map<String, String> out = new HashMap<>();
                String json = rs.getString(column);
                if (json != null) {
                    JSONObject obj = new JSONObject(json);
                    for (String key : obj.keySet()) {
                        out.put(key, obj.getString(key));
                    }
                }


                return out;
            }
        });

        jdbc = new JdbcTemplate(db);
        ReflectionTestUtil.setFieldValue(connector, "template", jdbc);
        ReflectionTestUtil.setFieldValue(connector, "dbutils", h2dbutils);
    }

    @Test
    public void testSimpleNodeImport() throws SQLException, ParseException {
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("Spot").add(jdbc);
        connector.readEntireDatabase();

        Assert.assertEquals(1, importer.size());
        importer.assertContains(place);
    }

    @Test
    public void testImportForSelectedCountries() throws SQLException, ParseException {
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("SpotHU").country("hu").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotDE").country("de").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotUS").country("us").add(jdbc);
        connector.readEntireDatabase("uk", "hu", "nl");

        Assert.assertEquals(1, importer.size());
        importer.assertContains(place);
    }

     @Test
    public void testImportance() throws SQLException {
         PlacexTestRow place1 = new PlacexTestRow("amenity", "cafe").name("Spot").rankSearch(10).add(jdbc);
         PlacexTestRow place2 = new PlacexTestRow("amenity", "cafe").name("Spot").importance(0.3).add(jdbc);

         connector.readEntireDatabase();

         Assert.assertEquals(0.5, importer.get(place1.getPlaceId()).getImportance(), 0.00001);
         Assert.assertEquals(0.3, importer.get(place2.getPlaceId()).getImportance(), 0.00001);
     }

     @Test
    public void testPlaceAddress() throws SQLException, ParseException {
        PlacexTestRow place = new PlacexTestRow("highway", "residential").name("Burg").rankAddress(26).rankSearch(26).add(jdbc);

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

        Assert.assertEquals("Le Coin", doc.getLocality().get("name"));
        Assert.assertEquals("Crampton", doc.getDistrict().get("name"));
        Assert.assertEquals("Grand Junction", doc.getCity().get("name"));
        Assert.assertEquals("Lost County", doc.getCounty().get("name"));
        Assert.assertEquals("Le Havre", doc.getState().get("name"));
    }

    @Test
    public void testPoiAddress() throws ParseException {
        PlacexTestRow parent = new PlacexTestRow("highway", "residential").name("Burg").rankAddress(26).rankSearch(26).add(jdbc);

        parent.addAddresslines(jdbc,
                new PlacexTestRow("place", "city").name("Grand Junction").rankAddress(16).add(jdbc));

        PlacexTestRow place = new PlacexTestRow("place", "house").name("House").parent(parent).add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(3, importer.size());
        importer.assertContains(place);

        PhotonDoc doc = importer.get(place);

        Assert.assertEquals("Burg", doc.getStreet().get("name"));
        Assert.assertNull(doc.getLocality());
        Assert.assertNull(doc.getDistrict());
        Assert.assertEquals("Grand Junction", doc.getCity().get("name"));
        Assert.assertNull(doc.getCounty());
        Assert.assertNull(doc.getState());
    }

    @Test
    public void testInterpolationAny() throws SQLException, ParseException {
        PlacexTestRow street = new PlacexTestRow("highway", "residential").name("La strada").rankAddress(26).rankSearch(26).add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(1, 11, "all").parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);

        connector.readEntireDatabase();

        Assert.assertEquals(10, importer.size());

        PlacexTestRow expect = new PlacexTestRow("place", "house_number").id(osmline.getPlaceId()).parent(street).osm("W", 23);

        for (int i = 2; i < 11; ++i) {
            importer.assertContains(expect.housenumber(i).centroid(0, (i - 1)*0.1));
        }

    }
}
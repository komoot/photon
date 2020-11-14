package de.komoot.photon.nominatim;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.testdb.CollectingImporter;
import de.komoot.photon.nominatim.testdb.PlacexTestRow;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
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
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("name", "Spot").add(jdbc);
        connector.readEntireDatabase();

        Assert.assertEquals(1, importer.size());
        importer.assertContains(place);
    }

    @Test
    public void testImportForSelectedCountries() throws SQLException, ParseException {
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("name", "SpotHU").country("hu").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("name", "SpotDE").country("de").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("name", "SpotUS").country("us").add(jdbc);
        connector.readEntireDatabase("uk", "hu", "nl");

        Assert.assertEquals(1, importer.size());
        importer.assertContains(place);
    }

     @Test
    public void testImportance() throws SQLException, ParseException {
         PlacexTestRow place1 = new PlacexTestRow("amenity", "cafe").name("name", "Spot").rankSearch(10).add(jdbc);
         PlacexTestRow place2 = new PlacexTestRow("amenity", "cafe").name("name", "Spot").importance(0.3).add(jdbc);

         connector.readEntireDatabase();

         Assert.assertEquals(0.5, importer.get(place1.getPlaceId()).getImportance(), 0.00001);
         Assert.assertEquals(0.3, importer.get(place2.getPlaceId()).getImportance(), 0.00001);
     }
}
package de.komoot.photon.importer.elasticsearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.importer.model.PhotonDoc;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Peter Karich
 */
public class ServerTest extends ESBaseTester {

    @Before
    public void setUp() {
        setUpES();
        deleteAll();
    }

    @Test
    public void testCreateSnapshot() {
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("name", "testing");
        PhotonDoc doc = PhotonDoc.create(1, "way", 1, nameMap);
        Importer instance = new Importer(getClient());
        instance.add(doc);
        instance.finish();
        refresh();
        assertEquals(1L, instance.count());

        try {
            getApiServer().createSnapshot("test");
            deleteAll();
            assertEquals(0L, instance.count());

            // ... an existing index can be only restored if it’s closed.
            // The restore operation automatically opens restored indices if they were closed 
            // and creates new indices if they didn’t exist in the cluster.            
            getApiServer().closeIndex();
            getApiServer().importSnapshot(getApiServer().getDumpDirectory().toURI().toString() + "/test.zip", "test");
            // getApiServer().waitForYellow();
            
            assertEquals(1L, instance.count());        
            // assertEquals(1, new Searcher(getClient()).search("testing", "en", null, null, 10, true).size());
        } finally {
            getApiServer().deleteSnapshot("test");
        }
    }
}

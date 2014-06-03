package de.komoot.photon.importer.elasticsearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.importer.model.PhotonDoc;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich
 */
public class ImporterTest extends ESBaseTester {

    @Before
    public void setUp() {
        setUpES();
        deleteAll();
    }

    @Test
    public void testAdd() {
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("name", "testing");
        PhotonDoc doc = PhotonDoc.create(1, "way", 1, nameMap);
        Importer instance = new Importer(getClient());
        instance.add(doc);
        instance.finish();
        
        refresh();
        assertEquals(1L, instance.count());
        
        assertEquals(1, new Searcher(getClient()).search("testing", "en", null, null, 10, true).size());
    }
}

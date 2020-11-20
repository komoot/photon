package de.komoot.photon.nominatim.testdb;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class CollectingImporter implements Importer {
    private List<PhotonDoc> docs = new ArrayList<>();
    private int finishCalled = 0;


    @Override
    public void add(PhotonDoc doc) {
        docs.add(doc);
    }

    @Override
    public void finish() {
        ++finishCalled;
    }

    public void assertFinishCalled(int num) {
        Assert.assertEquals(num, finishCalled);
    }

    public int size() { return docs.size(); }

    public PhotonDoc get(PlacexTestRow row) {
        return get(row.getPlaceId());
    }

    public PhotonDoc get(long placeId) {
        for (PhotonDoc doc : docs) {
            if (doc.getPlaceId() == placeId) {
                return doc;
            }
        }

        Assert.fail("No document found with that place_id.");
        return null;
    }

    public void assertContains(PlacexTestRow row) throws ParseException {
        PhotonDoc doc = null;
        for (PhotonDoc outdoc : docs) {
            if (outdoc.getPlaceId() == row.getPlaceId()) {
                Assert.assertNull("Row is contained multiple times", doc);
                doc = outdoc;
            }
        }

        Assert.assertNotNull("Row not found", doc);

        Assert.assertEquals(row.getOsmType(), doc.getOsmType());
        Assert.assertEquals(row.getOsmId(), (Long) doc.getOsmId());
        Assert.assertEquals(row.getKey(), doc.getTagKey());
        Assert.assertEquals(row.getValue(), doc.getTagValue());
        Assert.assertEquals(row.getRankAddress(), (Integer) doc.getRankAddress());
        Assert.assertEquals(new WKTReader().read(row.getCentroid()), doc.getCentroid());
        Assert.assertEquals(row.getNames(), doc.getName());
    }
}

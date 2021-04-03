package de.komoot.photon.nominatim.testdb;

import com.vividsolutions.jts.io.ParseException;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    public int size() {
        return docs.size();
    }

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

        row.assertEquals(doc);
    }

    public void assertContains(PlacexTestRow row, int housenumber) throws ParseException {
        String hnrstr = Integer.toString(housenumber);
        PhotonDoc doc = null;
        for (PhotonDoc outdoc : docs) {
            if (outdoc.getPlaceId() == row.getPlaceId() && hnrstr.equals(outdoc.getHouseNumber())) {
                Assert.assertNull("Row is contained multiple times", doc);
                doc = outdoc;
            }
        }

        Assert.assertNotNull("Row not found", doc);

        row.assertEquals(doc);
    }}

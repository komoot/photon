package de.komoot.photon.elasticsearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import org.elasticsearch.action.get.GetResponse;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ImporterTest extends ESBaseTester {

    @After
    public void tearDown() {
        deleteIndex();
        shutdownES();
    }

    @Test
    public void addSimpleDoc() throws IOException {
        setUpES();
        Importer instance = new Importer(getClient(), "en");
        instance.add(new PhotonDoc(1234, "N", 1000, "place", "city"));
        instance.finish();
        refresh();

        GetResponse response = getById(1234);

        assertTrue(response.isExists());
    }
}
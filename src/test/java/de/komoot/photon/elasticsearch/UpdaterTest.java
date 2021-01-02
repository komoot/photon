package de.komoot.photon.elasticsearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import org.elasticsearch.action.get.GetResponse;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UpdaterTest extends ESBaseTester {

    private GetResponse getById(int id) {
        return getClient().prepareGet(PhotonIndex.NAME,PhotonIndex.TYPE, String.valueOf(id))
                .execute()
                .actionGet();
    }

    @After
    public void tearDown() {
        deleteIndex();
        shutdownES();
    }


    @Test
    public void addNameToDoc() throws IOException {
        Map<String, String> names = new HashMap<>();
        names.put("name", "Foo");
        PhotonDoc doc = new PhotonDoc(1234, "N", 1000, "place", "city").names(names);

        setUpES();
        Importer instance = new Importer(getClient(), "en");
        instance.add(doc);
        instance.finish();
        refresh();

        names.put("name:en", "Enfoo");
        Updater updater = new Updater(getClient(), "en,de");
        updater.updateOrCreate(doc);
        updater.finish();
        refresh();

        GetResponse response = getById(1234);

        assertTrue(response.isExists());
        Map<String, String> out_names = (Map<String, String>) response.getSourceAsMap().get("name");
        assertEquals("Foo", out_names.get("default"));
        assertEquals("Enfoo", out_names.get("en"));
    }

    @Test
    public void removeNameFromDoc() throws IOException {
        Map<String, String> names = new HashMap<>();
        names.put("name", "Foo");
        names.put("name:en", "Enfoo");
        PhotonDoc doc = new PhotonDoc(1234, "N", 1000, "place", "city").names(names);

        setUpES();
        Importer instance = new Importer(getClient(), "en");
        instance.add(doc);
        instance.finish();
        refresh();

        names.remove("name");
        Updater updater = new Updater(getClient(), "en,de");
        updater.updateOrCreate(doc);
        updater.finish();
        refresh();

        GetResponse response = getById(1234);

        assertTrue(response.isExists());
        Map<String, String> out_names = (Map<String, String>) response.getSourceAsMap().get("name");
        //assertFalse(out_names.containsKey("default"));
        assertEquals("Enfoo", out_names.get("en"));
    }
}
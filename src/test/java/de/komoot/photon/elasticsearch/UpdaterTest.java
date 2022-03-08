package de.komoot.photon.elasticsearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UpdaterTest extends ESBaseTester {

    @Test
    public void addNameToDoc() throws IOException {
        Map<String, String> names = new HashMap<>();
        names.put("name", "Foo");
        PhotonDoc doc = new PhotonDoc(1234, "N", 1000, "place", "city").names(names);

        setUpES();
        Importer instance = makeImporter();
        instance.add(doc);
        instance.finish();
        refresh();

        names.put("name:en", "Enfoo");
        Updater updater = makeUpdater();
        updater.create(doc);
        updater.finish();
        refresh();

        PhotonResult response = getById(1234);
        assertNotNull(response);

        Map<String, String> out_names = response.getMap("name");
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
        Importer instance = makeImporter();
        instance.add(doc);
        instance.finish();
        refresh();

        names.remove("name");
        Updater updater = makeUpdater();
        updater.create(doc);
        updater.finish();
        refresh();

        PhotonResult response = getById(1234);
        assertNotNull(response);

        Map<String, String> out_names = response.getMap("name");
        assertFalse(out_names.containsKey("default"));
        assertEquals("Enfoo", out_names.get("en"));
    }

    @Test
    public void addExtraTagsToDoc() throws IOException {
        Map<String, String> names = new HashMap<>();
        names.put("name", "Foo");
        PhotonDoc doc = new PhotonDoc(1234, "N", 1000, "place", "city").names(names);

        setUpES();
        Importer instance = makeImporterWithExtra("website");
        instance.add(doc);
        instance.finish();
        refresh();

        PhotonResult response = getById(1234);
        assertNotNull(response);

        assertNull(response.get("extra"));

        doc.extraTags(Collections.singletonMap("website", "http://site.foo"));
        Updater updater = makeUpdaterWithExtra("website");
        updater.create(doc);
        updater.finish();
        refresh();

        response = getById(1234);
        assertNotNull(response);

        Map<String, String> extra = response.getMap("extra");

        assertNotNull(extra);
        assertEquals(Collections.singletonMap("website", "http://site.foo"), extra);
    }
}
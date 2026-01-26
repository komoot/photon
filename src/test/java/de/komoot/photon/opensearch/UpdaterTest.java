package de.komoot.photon.opensearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdaterTest extends ESBaseTester {

    @TempDir
    private Path dataDirectory;

    private PhotonDoc createDoc(String... names) {
        return new PhotonDoc()
                .placeId("1234").osmType("N").osmId(1000).tagKey("place").tagValue("city")
                .names(makeDocNames(names));
    }

    @Test
    void addNameToDoc() throws IOException {
        PhotonDoc doc = createDoc("name", "Foo");

        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(doc));
        instance.finish();
        refresh();

        doc.names(makeDocNames("name", "Foo", "name:en", "Enfoo"));
        Updater updater = makeUpdater();
        updater.addOrUpdate(List.of(doc));
        updater.finish();
        refresh();

        PhotonResult response = getById(1234);
        assertNotNull(response);

        Map<String, String> outNames = response.getMap("name");
        assertEquals("Foo", outNames.get("default"));
        assertEquals("Enfoo", outNames.get("en"));
    }

    @Test
    void removeNameFromDoc() throws IOException {
        PhotonDoc doc = createDoc("name", "Foo", "name:en", "Enfoo");

        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(doc));
        instance.finish();
        refresh();

        doc.names(makeDocNames("name:en", "Enfoo"));
        Updater updater = makeUpdater();
        updater.addOrUpdate(List.of(doc));
        updater.finish();
        refresh();

        PhotonResult response = getById(1234);
        assertNotNull(response);

        Map<String, String> outNames = response.getMap("name");
        assertFalse(outNames.containsKey("default"));
        assertEquals("Enfoo", outNames.get("en"));
    }

    @Test
    void addExtraTagsToDoc() throws IOException {
        PhotonDoc doc = createDoc("name", "Foo");

        getProperties().setExtraTags(List.of("website"));
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(doc));
        instance.finish();
        refresh();

        PhotonResult response = getById(1234);
        assertNotNull(response);

        assertNull(response.get("extra"));

        doc.extraTags(Map.of("website", "http://site.foo"));
        Updater updater = makeUpdater();
        updater.addOrUpdate(List.of(doc));
        updater.finish();
        refresh();

        response = getById(1234);
        assertNotNull(response);

        Map<String, String> extra = response.getMap("extra");

        assertNotNull(extra);
        assertEquals(Map.of("website", "http://site.foo"), extra);
    }

    @Test
    void deleteDoc() throws IOException {
        getProperties().setExtraTags(List.of("website"));
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(
                createDoc().houseNumber("34"),
                createDoc().houseNumber("35")));
        instance.finish();
        refresh();

        assertNotNull(getById("1234"));
        assertNotNull(getById("1234.1"));

        Updater updater = makeUpdater();
        updater.delete("1234");
        updater.finish();
        refresh();

        assertNotNull(getById("1234"));
        assertNull(getById("1234.1"));
    }
}
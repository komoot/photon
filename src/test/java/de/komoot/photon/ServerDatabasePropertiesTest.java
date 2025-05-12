package de.komoot.photon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ServerDatabasePropertiesTest extends ESBaseTester {

    @Test
    void testSaveAndLoadFromDatabase(@TempDir Path dataDirectory) throws IOException {
        setUpES(dataDirectory);

        final Date now = new Date();

        DatabaseProperties prop = new DatabaseProperties();
        prop.setLanguages(new String[]{"en", "de", "fr"});
        prop.setImportDate(now);
        prop.setSupportGeometries(true);

        getServer().saveToDatabase(prop);

        prop = getServer().loadFromDatabase();

        assertArrayEquals(new String[]{"en", "de", "fr"}, prop.getLanguages());
        assertEquals(now, prop.getImportDate());
        assertTrue(prop.getSupportGeometries());
    }
}
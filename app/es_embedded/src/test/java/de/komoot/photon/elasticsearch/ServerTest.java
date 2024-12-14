package de.komoot.photon.elasticsearch;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.ESBaseTester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest extends ESBaseTester {

    @Test
    void testSaveAndLoadFromDatabase() throws IOException {
        setUpES();

        Date now = new Date();
        DatabaseProperties prop = new DatabaseProperties(new String[]{"en", "de", "fr"}, now, false, false);
        getServer().saveToDatabase(prop);

        prop = getServer().loadFromDatabase();

        assertArrayEquals(new String[]{"en", "de", "fr"}, prop.getLanguages());
        assertEquals(now, prop.getImportDate());

    }
}
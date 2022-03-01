package de.komoot.photon.elasticsearch;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.ESBaseTester;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest extends ESBaseTester {

    @Test
    public void testSaveAndLoadFromDatabase() throws IOException {
        setUpES();

        DatabaseProperties prop = new DatabaseProperties();
        prop.setLanguages(new String[]{"en", "de", "fr"});
        getServer().saveToDatabase(prop);

        prop = new DatabaseProperties();
        getServer().loadFromDatabase(prop);

        assertArrayEquals(new String[]{"en", "de", "fr"}, prop.getLanguages());


    }
}
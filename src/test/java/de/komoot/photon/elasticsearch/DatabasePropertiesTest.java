package de.komoot.photon.elasticsearch;

import de.komoot.photon.ESBaseTester;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests for the database-global property store.
 */
public class DatabasePropertiesTest extends ESBaseTester {

    /**
     * setLanguages() overwrites the language settings.
     */
    @Test
    public void testSetLanguages() {
        DatabaseProperties prop = new DatabaseProperties();

        prop.setLanguages(new String[]{"en", "bg", "de"});
        assertArrayEquals(new String[]{"en", "bg", "de"}, prop.getLanguages());

        prop.setLanguages(new String[]{"ru"});
        assertArrayEquals(new String[]{"ru"}, prop.getLanguages());
    }

    /**
     * If languages is not set, then the restricted language set is used as is.
     */
    @Test
    public void testRestrictLanguagesUnsetLanguages() {
        DatabaseProperties prop = new DatabaseProperties();
        prop.restrictLanguages(new String[]{"en", "bg", "de"});

        assertArrayEquals(new String[]{"en", "bg", "de"}, prop.getLanguages());
    }

    /**
     * When languages are set, then only the languages of the restricted set are used
     * that already exist and the order of the input is preserved.
     */
    @Test
    public void testRestrictLanguagesAlreadySet() {
        DatabaseProperties prop = new DatabaseProperties();
        prop.setLanguages(new String[]{"en", "de", "fr"});

        prop.restrictLanguages(new String[]{"cn", "de", "en", "es"});

        assertArrayEquals(new String[]{"de", "en"}, prop.getLanguages());
    }

    @Test
    public void testSaveAndLoadFromDatabase() throws IOException {
        setUpES();

        DatabaseProperties prop = new DatabaseProperties();
        prop.setLanguages(new String[]{"en", "de", "fr"});
        prop.saveToDatabase(getClient());

        prop = new DatabaseProperties();
        prop.loadFromDatabase(getClient());

        assertArrayEquals(new String[]{"en", "de", "fr"}, prop.getLanguages());


    }
}

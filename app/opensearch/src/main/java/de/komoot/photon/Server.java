package de.komoot.photon;

import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.searcher.SearchHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Date;

public class Server {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Server.class);

    public Server(String mainDirectory) {
    }

    public Server start(String clusterName, String[] transportAddresses) {
        return this;
    }

    public void waitForReady() {

    }

    public void refreshIndexes() {

    }

    public void shutdown() {

    }

    public DatabaseProperties recreateIndex(String[] languages, Date importDate) throws IOException {
        return null;
    }

    public void updateIndexSettings(String synonymFile) throws IOException {

    }

    public void saveToDatabase(DatabaseProperties dbProperties) throws IOException {

    }

    public void loadFromDatabase(DatabaseProperties dbProperties) {

    }

    public Importer createImporter(String[] languages, String[] extraTags) {
        return null;
    }

    public Updater createUpdater(String[] languages, String[] extraTags) {
        return null;
    }

    public SearchHandler createSearchHandler(String[] languages, int queryTimeoutSec) {
        return null;
    }

    public ReverseHandler createReverseHandler(int queryTimeoutSec) {
        return null;
    }

}

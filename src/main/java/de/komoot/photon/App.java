package de.komoot.photon;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.komoot.photon.elasticsearch.Server;
import de.komoot.photon.nominatim.NominatimConnector;
import de.komoot.photon.nominatim.NominatimUpdater;
import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.utils.CorsFilter;
import org.slf4j.Logger;
import spark.Request;
import spark.Response;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import static spark.Spark.*;

/**
 * Main Photon application.
 */
public class App {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(App.class);

    public static void main(String[] rawArgs) throws Exception {
        CommandLineArgs args = parseCommandLine(rawArgs);

        if (args.getJsonDump() != null) {
            startJsonDump(args);
            return;
        }

        if (args.getNominatimUpdateInit() != null) {
            startNominatimUpdateInit(args);
            return;
        }

        boolean shutdownES = false;
        final Server esServer = new Server(args.getDataDirectory()).start(args.getCluster(), args.getTransportAddresses());
        try {
            LOGGER.info("Make sure that the ES cluster is ready, this might take some time.");
            esServer.waitForReady();
            LOGGER.info("ES cluster is now ready.");

            if (args.isNominatimImport()) {
                shutdownES = true;
                startNominatimImport(args, esServer);
                return;
            }

            // Working on an existing installation.
            // Update the index settings in case there are any changes.
            esServer.updateIndexSettings(args.getSynonymFile());
            esServer.waitForReady();

            if (args.isNominatimUpdate()) {
                shutdownES = true;
                startNominatimUpdate(setupNominatimUpdater(args, esServer), esServer);
                return;
            }

            // No special action specified -> normal mode: start search API
            startApi(args, esServer);
        } finally {
            if (shutdownES) esServer.shutdown();
        }
    }


    private static CommandLineArgs parseCommandLine(String[] rawArgs) {
        CommandLineArgs args = new CommandLineArgs();
        final JCommander jCommander = new JCommander(args);
        try {
            jCommander.parse(rawArgs);

            // Cors arguments are mutually exclusive.
            if (args.isCorsAnyOrigin() && args.getCorsOrigin() != null) {
                throw new ParameterException("Use only one cors configuration type");
            }
        } catch (ParameterException e) {
            LOGGER.warn("Could not start photon: {}", e.getMessage());
            jCommander.usage();
            System.exit(1);
        }

        // show help
        if (args.isUsage()) {
            jCommander.usage();
            System.exit(1);
        }

        return args;
    }


    /**
     * Take nominatim data and dump it to a Json file.
     */
    private static void startJsonDump(CommandLineArgs args) {
        try {
            final String filename = args.getJsonDump();
            final JsonDumper jsonDumper = new JsonDumper(filename, args.getLanguages(), args.getExtraTags());
            NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
            nominatimConnector.setImporter(jsonDumper);
            nominatimConnector.readEntireDatabase(args.getCountryCodes());
            LOGGER.info("Json dump was created: {}", filename);
        } catch (FileNotFoundException e) {
            LOGGER.error("Cannot create dump", e);
        }
    }


    /**
     * Read all data from a Nominatim database and import it into a Photon database.
     */
    private static void startNominatimImport(CommandLineArgs args, Server esServer) {
        DatabaseProperties dbProperties;
        NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        Date importDate = nominatimConnector.getLastImportDate();
        try {
            dbProperties = esServer.recreateIndex(args.getLanguages(), importDate); // clear out previous data
        } catch (IOException e) {
            throw new RuntimeException("Cannot setup index, elastic search config files not readable", e);
        }

        LOGGER.info("Starting import from nominatim to photon with languages: {}", String.join(",", dbProperties.getLanguages()));
        nominatimConnector.setImporter(esServer.createImporter(dbProperties.getLanguages(), args.getExtraTags()));
        nominatimConnector.readEntireDatabase(args.getCountryCodes());

        LOGGER.info("Imported data from nominatim to photon with languages: {}", String.join(",", dbProperties.getLanguages()));
    }

    private static void startNominatimUpdateInit(CommandLineArgs args) {
        NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        nominatimUpdater.initUpdates(args.getNominatimUpdateInit());
    }

    private static void startNominatimUpdate(NominatimUpdater nominatimUpdater, Server esServer) {
        nominatimUpdater.update();

        DatabaseProperties dbProperties = new DatabaseProperties();
        esServer.loadFromDatabase(dbProperties);
        Date importDate = nominatimUpdater.getLastImportDate();
        dbProperties.setImportDate(importDate);
        try {
            esServer.saveToDatabase(dbProperties);
        } catch (IOException e) {
            throw new RuntimeException("Cannot setup index, elastic search config files not readable", e);
        }
    }


    /**
     * Prepare Nominatim updater.
     */
    private static NominatimUpdater setupNominatimUpdater(CommandLineArgs args, Server server) {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = new DatabaseProperties();
        server.loadFromDatabase(dbProperties);

        NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        nominatimUpdater.setUpdater(server.createUpdater(dbProperties.getLanguages(), args.getExtraTags()));
        return nominatimUpdater;
    }

    /**
     * Start API server to accept search requests via http.
     */
    private static void startApi(CommandLineArgs args, Server server) {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = new DatabaseProperties();
        server.loadFromDatabase(dbProperties);
        if (args.getLanguages(false).length > 0) {
            dbProperties.restrictLanguages(args.getLanguages());
        }

        port(args.getListenPort());
        ipAddress(args.getListenIp());

        String allowedOrigin = args.isCorsAnyOrigin() ? "*" : args.getCorsOrigin();
        if (allowedOrigin != null) {
            CorsFilter.enableCORS(allowedOrigin, "get", "*");
        } else {
            // Set Json content type. In the other case already set by enableCors.
            before((request, response) -> response.type("application/json; charset=UTF-8"));
        }

        // setup search API
        String[] langs = dbProperties.getLanguages();

        SearchHandler searchHandler = server.createSearchHandler(langs, args.getQueryTimeout());
        get("api", new SearchRequestHandler("api", searchHandler, langs, args.getDefaultLanguage()));
        get("api/", new SearchRequestHandler("api/", searchHandler, langs, args.getDefaultLanguage()));

        ReverseHandler reverseHandler = server.createReverseHandler(args.getQueryTimeout());
        get("reverse", new ReverseSearchRequestHandler("reverse", reverseHandler, dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("reverse/", new ReverseSearchRequestHandler("reverse/", reverseHandler, dbProperties.getLanguages(), args.getDefaultLanguage()));
        
        get("status", new StatusRequestHandler("status", server));
        get("status/", new StatusRequestHandler("status/", server));

        if (args.isEnableUpdateApi()) {
            // setup update API
            final NominatimUpdater nominatimUpdater = setupNominatimUpdater(args, server);
            if (!nominatimUpdater.isSetUpForUpdates()) {
                throw new RuntimeException("Update API enabled, but Nominatim database is not prepared. Run -nominiatim-update-init-for first.");
            }
            get("/nominatim-update/status", (Request request, Response response) -> {
               if (nominatimUpdater.isBusy()) {
                   return "\"BUSY\"";
               }

               return "\"OK\"";
            });
            get("/nominatim-update", (Request request, Response response) -> {
                new Thread(()-> App.startNominatimUpdate(nominatimUpdater, server)).start();
                return "\"nominatim update started (more information in console output) ...\"";
            });
        }
    }
}

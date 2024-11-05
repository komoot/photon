package de.komoot.photon;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.komoot.photon.nominatim.ImportThread;
import de.komoot.photon.nominatim.NominatimConnector;
import de.komoot.photon.nominatim.NominatimUpdater;
import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.searcher.SearchHandler;
import de.komoot.photon.searcher.StructuredSearchHandler;
import de.komoot.photon.utils.CorsFilter;
import org.slf4j.Logger;
import spark.Request;
import spark.Response;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static spark.Spark.*;

/**
 * Main Photon application.
 */
public class App {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(App.class);

    public static void main(String[] rawArgs) throws Exception {
        CommandLineArgs args = parseCommandLine(rawArgs);

        try {
            runPhoton(args);
        } catch (UsageException e) {
            LOGGER.error(e.getMessage());
            LOGGER.error("Exiting.");
            System.exit(2);
        }
    }

    private static void runPhoton(CommandLineArgs args) throws IOException {
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
            esServer.refreshIndexes();

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

            importFromDatabase(args, jsonDumper);
            LOGGER.info("Json dump was created: {}", filename);
        } catch (FileNotFoundException e) {
            throw new UsageException("Cannot create dump: " + e.getMessage());
        }
    }


    /**
     * Read all data from a Nominatim database and import it into a Photon database.
     */
    private static void startNominatimImport(CommandLineArgs args, Server esServer) {
        final var languages = initDatabase(args, esServer);

        LOGGER.info("Starting import from nominatim to photon with languages: {}", String.join(",", languages));
        importFromDatabase(args, esServer.createImporter(languages, args.getExtraTags()));

        LOGGER.info("Imported data from nominatim to photon with languages: {}", String.join(",", languages));
    }

    private static String[] initDatabase(CommandLineArgs args, Server esServer) {
        final var nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        final Date importDate = nominatimConnector.getLastImportDate();

        try {
            // Clear out previous data.
            var dbProperties = esServer.recreateIndex(args.getLanguages(), importDate, args.getSupportStructuredQueries());
            return dbProperties.getLanguages();
        } catch (IOException e) {
            throw new UsageException("Cannot setup index, elastic search config files not readable");
        }
    }

    private static void importFromDatabase(CommandLineArgs args, Importer importer) {
        final var connector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        connector.prepareDatabase();
        connector.loadCountryNames();

        String[] countries = args.getCountryCodes();

        if (countries == null || countries.length == 0) {
            countries = connector.getCountriesFromDatabase();
        } else {
            countries = Arrays.stream(countries).map(String::trim).filter(s -> !s.isBlank()).toArray(String[]::new);
        }

        final int numThreads = args.getThreads();
        ImportThread importThread = new ImportThread(importer);

        try {

            if (numThreads == 1) {
                for (var country : countries) {
                    connector.readCountry(country, importThread);
                }
            } else {
                final Queue<String> todolist = new ConcurrentLinkedQueue<>(List.of(countries));

                final List<Thread> readerThreads = new ArrayList<>(numThreads);

                for (int i = 0; i < numThreads; ++i) {
                    final NominatimConnector threadConnector;
                    if (i > 0) {
                        threadConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
                        threadConnector.loadCountryNames();
                    } else {
                        threadConnector = connector;
                    }
                    final int threadno = i;
                    Runnable runner = () -> {
                        String nextCc = todolist.poll();
                        while (nextCc != null) {
                            LOGGER.info("Thread {}: reading country '{}'", threadno, nextCc);
                            threadConnector.readCountry(nextCc, importThread);
                            nextCc = todolist.poll();
                        }
                    };
                    Thread thread = new Thread(runner);
                    thread.start();
                    readerThreads.add(thread);
                }
                readerThreads.forEach(t -> {
                    while (true) {
                        try {
                            t.join();
                            break;
                        } catch (InterruptedException e) {
                            LOGGER.warn("Thread interrupted:", e);
                            // Restore interrupted state.
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
        } finally {
            importThread.finish();
        }

    }


    private static void startNominatimUpdateInit(CommandLineArgs args) {
        NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        nominatimUpdater.initUpdates(args.getNominatimUpdateInit());
    }

    private static void startNominatimUpdate(NominatimUpdater nominatimUpdater, Server esServer)  {
        nominatimUpdater.update();

        try {
            DatabaseProperties dbProperties = esServer.loadFromDatabase();
            Date importDate = nominatimUpdater.getLastImportDate();
            dbProperties.setImportDate(importDate);
            esServer.saveToDatabase(dbProperties);
        } catch (IOException e) {
            throw new UsageException("Cannot setup index, elastic search config files not readable");
        }
    }


    /**
     * Prepare Nominatim updater.
     */
    private static NominatimUpdater setupNominatimUpdater(CommandLineArgs args, Server server)throws IOException {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = server.loadFromDatabase();

        NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        nominatimUpdater.setUpdater(server.createUpdater(dbProperties.getLanguages(), args.getExtraTags()));
        return nominatimUpdater;
    }

    /**
     * Start API server to accept search requests via http.
     */
    private static void startApi(CommandLineArgs args, Server server) throws IOException {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = server.loadFromDatabase();
        if (args.getLanguages(false).length > 0) {
            dbProperties.restrictLanguages(args.getLanguages());
        }

        port(args.getListenPort());
        ipAddress(args.getListenIp());

        String[] allowedOrigin = args.isCorsAnyOrigin() ? new String[]{ "*" } : args.getCorsOrigin();
        if (allowedOrigin.length > 0) {
            CorsFilter.enableCORS(allowedOrigin, "get", "*");
        } else {
            // Set Json content type. In the other case already set by enableCors.
            before((request, response) -> response.type("application/json; charset=UTF-8"));
        }

        // setup search API
        String[] langs = dbProperties.getLanguages();

        SearchHandler searchHandler = server.createSearchHandler(langs, args.getQueryTimeout());
        get("api", new SearchRequestHandler("api", searchHandler, langs, args.getDefaultLanguage(), args.getMaxResults()));
        get("api/", new SearchRequestHandler("api/", searchHandler, langs, args.getDefaultLanguage(), args.getMaxResults()));

        if (dbProperties.getSupportStructuredQueries()) {
            StructuredSearchHandler structured = server.createStructuredSearchHandler(langs, args.getQueryTimeout());
            get("structured", new StructuredSearchRequestHandler("structured", structured, langs, args.getDefaultLanguage(), args.getMaxResults()));
            get("structured/", new StructuredSearchRequestHandler("structured/", structured, langs, args.getDefaultLanguage(), args.getMaxResults()));
        }

        ReverseHandler reverseHandler = server.createReverseHandler(args.getQueryTimeout());
        get("reverse", new ReverseSearchRequestHandler("reverse", reverseHandler, dbProperties.getLanguages(),
                args.getDefaultLanguage(), args.getMaxReverseResults()));
        get("reverse/", new ReverseSearchRequestHandler("reverse/", reverseHandler, dbProperties.getLanguages(),
                args.getDefaultLanguage(), args.getMaxReverseResults()));
        
        get("status", new StatusRequestHandler("status", server));
        get("status/", new StatusRequestHandler("status/", server));

        if (args.isEnableUpdateApi()) {
            // setup update API
            final NominatimUpdater nominatimUpdater = setupNominatimUpdater(args, server);
            if (!nominatimUpdater.isSetUpForUpdates()) {
                throw new UsageException("Update API enabled, but Nominatim database is not prepared. Run -nominatim-update-init-for first.");
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

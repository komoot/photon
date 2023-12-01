package de.komoot.photon;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.komoot.photon.elasticsearch.ElasticsearchServer;
import de.komoot.photon.elasticsearch.IndexMapping;
import de.komoot.photon.elasticsearch.IndexSettings;
import de.komoot.photon.nominatim.NominatimConnector;
import de.komoot.photon.nominatim.NominatimUpdater;
import de.komoot.photon.utils.CorsFilter;
import lombok.extern.slf4j.Slf4j;
import spark.Request;
import spark.Response;

import java.io.FileNotFoundException;
import java.io.IOException;

import static spark.Spark.*;


@Slf4j
public class App {

    public static void main(String[] rawArgs) throws Exception {
        CommandLineArgs args = parseCommandLine(rawArgs);

        if (args.getJsonDump() != null) {
            startJsonDump(args);
            return;
        }

        final ElasticsearchServer esServer = new ElasticsearchServer(args.getServerUrl())
                .apiKey(args.getApiKey())
                .start();

        log.info("Make sure that the ES cluster is ready, this might take some time.");
        esServer.waitForReady();

        if (args.isNominatimImport()) {
            log.info("ES cluster is now ready for import.");
            startNominatimImport(args, esServer);
            return;
        }

        // Working on an existing installation.
        // Update the index settings in case there are any changes.
        log.info("Refreshing index settings.");
        esServer.updateSettings(IndexSettings.buildSettings(args.getSynonymFile()));
        esServer.waitForReady();
        log.info("ES cluster is now ready.");

        if (args.isNominatimUpdate()) {
            final NominatimUpdater nominatimUpdater = setupNominatimUpdater(args, esServer);
            nominatimUpdater.update();
            return;
        }

        // no special action specified -> normal mode: start search API
        startApi(args, esServer);
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

            if (args.getServerUrl() == null) {
                throw new ParameterException("serverUrl is a required parameter");
            }
        } catch (ParameterException e) {
            log.warn("could not start photon: " + e.getMessage());
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
     * take nominatim data and dump it to json
     *
     * @param args
     */
    private static void startJsonDump(CommandLineArgs args) {
        try {
            final String filename = args.getJsonDump();
            final JsonDumper jsonDumper = new JsonDumper(filename, args.getLanguages(), args.getExtraTags(), args.isAllExtraTags());
            NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
            nominatimConnector.setImporter(jsonDumper);
            nominatimConnector.readEntireDatabase(args.getCountryCodes());
            log.info("json dump was created: " + filename);
        } catch (FileNotFoundException e) {
            log.error("cannot create dump", e);
        }
    }


    /**
     * Read all data from a Nominatim database and import it into a Photon database.
     */
    private static void startNominatimImport(CommandLineArgs args, ElasticsearchServer esServer) {
        try {
            esServer.recreateIndex(
                    IndexSettings.buildSettings(args.getSynonymFile()),
                    IndexMapping.buildMappings(args.getLanguages()),
                    args.getLanguages()
            ); // clear out previous data
        } catch (IOException e) {
            throw new RuntimeException("cannot setup index, elastic search config files not readable", e);
        }


        log.info("starting import from nominatim to photon with languages: " + String.join(",", args.getLanguages()));
        NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        nominatimConnector.setImporter(esServer.createImporter(args.getLanguages(), args.getExtraTags(), args.isAllExtraTags()));
        nominatimConnector.readEntireDatabase(args.getCountryCodes());

        log.info("imported data from nominatim to photon with languages: " + String.join(",", args.getLanguages()));
    }

    /**
     * Prepare Nominatim updater.
     */
    private static NominatimUpdater setupNominatimUpdater(CommandLineArgs args, ElasticsearchServer server) throws IOException {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = server.loadDbProperties();

        NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        nominatimUpdater.setUpdater(server.createUpdater(dbProperties.getLanguages(), args.getExtraTags(), args.isAllExtraTags()));
        return nominatimUpdater;
    }

    /**
     * Start API server to accept search requests via http.
     */
    private static void startApi(CommandLineArgs args, ElasticsearchServer server) throws IOException {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = server.loadDbProperties();

        if (args.getLanguages(false).length > 0) {
            dbProperties.restrictLanguages(args.getLanguages());
        }

        port(args.getListenPort());
        ipAddress(args.getListenIp());

        String allowedOrigin = args.isCorsAnyOrigin() ? "*" : args.getCorsOrigin();
        if (allowedOrigin != null) {
            CorsFilter.enableCORS(allowedOrigin, "get", "*");
        } else {
            before((request, response) -> {
                response.type("application/json; charset=UTF-8"); // in the other case set by enableCors
            });
        }

        // setup search API
        String[] langs = dbProperties.getLanguages();
        get("api", new SearchRequestHandler("api", server.createSearchHandler(langs), langs, args.getDefaultLanguage()));
        get("api/", new SearchRequestHandler("api/", server.createSearchHandler(langs), langs, args.getDefaultLanguage()));
        get("reverse", new ReverseSearchRequestHandler("reverse", server.createReverseHandler(), dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("reverse/", new ReverseSearchRequestHandler("reverse/", server.createReverseHandler(), dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("lookup", new LookupSearchRequestHandler("lookup", server.createLookupHandler(), dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("lookup/", new LookupSearchRequestHandler("lookup/", server.createLookupHandler(), dbProperties.getLanguages(), args.getDefaultLanguage()));

        if (args.isEnableUpdateApi()) {
            // setup update API
            final NominatimUpdater nominatimUpdater = setupNominatimUpdater(args, server);
            get("/nominatim-update", (Request request, Response response) -> {
                new Thread(nominatimUpdater::update).start();
                return "nominatim update started (more information in console output) ...";
            });
        }
    }
}

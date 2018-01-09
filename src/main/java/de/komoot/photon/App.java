package de.komoot.photon;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.komoot.photon.elasticsearch.Server;
import de.komoot.photon.nominatim.NominatimConnector;
import de.komoot.photon.nominatim.NominatimUpdater;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;
import spark.Request;
import spark.Response;

import java.io.FileNotFoundException;
import java.io.IOException;

import static spark.Spark.*;


@Slf4j
public class App {

    public static void main(String[] rawArgs) throws Exception {
        // parse command line arguments
        CommandLineArgs args = new CommandLineArgs();
        final JCommander jCommander = new JCommander(args);
        try {
            jCommander.parse(rawArgs);
        } catch (ParameterException e) {
            log.warn("could not start photon: " + e.getMessage());
            jCommander.usage();
            return;
        }

        // show help
        if (args.isUsage()) {
            jCommander.usage();
            return;
        }

        if (args.getJsonDump() != null) {
            startJsonDump(args);
            return;
        }

        boolean shutdownES = false;
        final Server esServer = new Server(args).start();
        try {
            Client esClient = esServer.getClient();

            if (args.isRecreateIndex()) {
                shutdownES = true;
                startRecreatingIndex(esServer);
                return;
            }

            if (args.isNominatimImport()) {
                shutdownES = true;
                startNominatimImport(args, esServer, esClient);
                return;
            }

            // no special action specified -> normal mode: start search API
            startApi(args, esClient);
        } finally {
            if (shutdownES) esServer.shutdown();
        }
    }


    /**
     * dump elastic search index and create a new and empty one
     *
     * @param esServer
     */
    private static void startRecreatingIndex(Server esServer) {
        try {
            esServer.recreateIndex();
        } catch (IOException e) {
            log.error("cannot setup index, elastic search config files not readable", e);
            return;
        }

        log.info("deleted photon index and created an empty new one.");
    }


    /**
     * take nominatim data and dump it to json
     *
     * @param args
     */
    private static void startJsonDump(CommandLineArgs args) {
        try {
            final String filename = args.getJsonDump();
            final JsonDumper jsonDumper = new JsonDumper(filename, args.getLanguages());
            NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
            nominatimConnector.setImporter(jsonDumper);
            nominatimConnector.readEntireDatabase(args.getCountryCodes().split(","));
            log.info("json dump was created: " + filename);
        } catch (FileNotFoundException e) {
            log.error("cannot create dump", e);
        }
    }


    /**
     * take nominatim data to fill elastic search index
     *
     * @param args
     * @param esServer
     * @param esNodeClient
     */
    private static void startNominatimImport(CommandLineArgs args, Server esServer, Client esNodeClient) {
        try {
            esServer.recreateIndex(); // dump previous data
        } catch (IOException e) {
            log.error("cannot setup index, elastic search config files not readable", e);
            return;
        }

        log.info("starting import from nominatim to photon with languages: " + args.getLanguages());
        de.komoot.photon.elasticsearch.Importer importer = new de.komoot.photon.elasticsearch.Importer(esNodeClient, args.getLanguages());
        NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        nominatimConnector.setImporter(importer);
        try {
            nominatimConnector.readEntireDatabase(args.getCountryCodes().split(","));
        } catch (Exception e) {
            log.info("error importing from nominatim: " + e.getMessage());
        }

        log.info("imported data from nominatim to photon with languages: " + args.getLanguages());
    }


    /**
     * start api to accept search requests via http
     *
     * @param args
     * @param esNodeClient
     */
    private static void startApi(CommandLineArgs args, Client esNodeClient) {
        setPort(args.getListenPort());
        setIpAddress(args.getListenIp());

        // setup search API
        get("api", new SearchRequestHandler("api", esNodeClient, args.getLanguages()));
        get("api/", new SearchRequestHandler("api/", esNodeClient, args.getLanguages()));
        get("reverse", new ReverseSearchRequestHandler("reverse", esNodeClient, args.getLanguages()));
        get("reverse/", new ReverseSearchRequestHandler("reverse/", esNodeClient, args.getLanguages()));

        // setup update API
        final NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        Updater updater = new de.komoot.photon.elasticsearch.Updater(esNodeClient, args.getLanguages());
        nominatimUpdater.setUpdater(updater);

        get("/nominatim-update", (Request request, Response response) -> {
            new Thread(() -> nominatimUpdater.update()).start();
            return "nominatim update started (more information in console output) ...";
        });
    }
}

package de.komoot.photon.importer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.komoot.photon.importer.elasticsearch.Importer;
import de.komoot.photon.importer.elasticsearch.Searcher;
import de.komoot.photon.importer.elasticsearch.Server;
import de.komoot.photon.importer.json.JsonDumper;
import de.komoot.photon.importer.nominatim.NominatimConnector;
import de.komoot.photon.importer.nominatim.NominatimUpdater;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.FileNotFoundException;
import java.io.IOException;

import static spark.Spark.*;

@Slf4j
public class App {
	public static void main(String[] rawArgs) {
		// parse command line arguments
		CommandLineArgs args = new CommandLineArgs();
		final JCommander jCommander = new JCommander(args);
		try {
			jCommander.parse(rawArgs);
		} catch(ParameterException e) {
			log.warn("could not start photon: " + e.getMessage());
			jCommander.usage();
			return;
		}

		// show help
		if(args.isUsage()) {
			jCommander.usage();
			return;
		}

		if(args.getJsonDump() != null) {
			try {
				final String filename = args.getJsonDump();
				final JsonDumper jsonDumper = new JsonDumper(filename, args.getLanguages());
				NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword(), args.getTagWhitelistFile());
				nominatimConnector.setImporter(jsonDumper);
				nominatimConnector.readEntireDatabase();
				log.info("json dump was created: " + filename);
				return;
			} catch(FileNotFoundException e) {
				log.error("cannot create dump", e);
			}
		}

		final Server esServer = new Server(args.getCluster(), args.getDataDirectory(), args.getLanguages());
		esServer.start();

		Client esNodeClient = esServer.getClient();

		if(args.isDeleteIndex()) {                        
                        try {
                                esServer.recreateIndex();
                        } catch (IOException e) {
                                log.error("cannot setup index, elastic search config files not readable", e);
                                return;
                        }
			
			log.info("deleted photon index and created an empty new one.");
			return;
		}

		if(args.isNominatimImport()) {
			try {
                                esServer.recreateIndex(); // dump previous data
                        } catch (IOException e) {
                                log.error("cannot setup index, elastic search config files not readable", e);
                                return;
                        }
                        
                        log.info("starting import from nominatim to photon with languages: " + args.getLanguages());
			Importer importer = new Importer(esNodeClient, args.getLanguages());
			NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword(), args.getTagWhitelistFile());
			nominatimConnector.setImporter(importer);
                        try {
                            nominatimConnector.readEntireDatabase();
                        } catch (Exception e) {
                            log.info("ERROR IMPORTING FROM NOMINATIM: "+e.getMessage());
                        }
			
                        log.info("imported data from nominatim to photon with languages: " + args.getLanguages());
			return;
		}

		final NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword(), args.getTagWhitelistFile());
		de.komoot.photon.importer.Updater updater = new de.komoot.photon.importer.elasticsearch.Updater(esNodeClient, args.getLanguages());
		nominatimUpdater.setUpdater(updater);

		startApi(args, esNodeClient, nominatimUpdater);
	}

	private static void startApi(CommandLineArgs args, Client esNodeClient, final NominatimUpdater nominatimUpdater) {
		setPort(args.getListenPort());
		setIpAddress(args.getListenIp());

		get(new Route("/nominatim-update") {
			@Override
			public Object handle(Request request, Response response) {
				Thread nominatimUpdaterThread = new Thread() {
					@Override
					public void run() {
						nominatimUpdater.update();
					}
				};
				nominatimUpdaterThread.start();
				return "nominatim update started (more information in console output) ...";
			}
		});

		final Searcher searcher = new Searcher(esNodeClient);
		get(new RequestHandler("api", searcher, args.getLanguages()));
		get(new RequestHandler("api/", searcher, args.getLanguages()));
	}
}

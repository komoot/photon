package de.komoot.photon.importer;

import com.beust.jcommander.JCommander;
import de.komoot.photon.importer.elasticsearch.ESUpdater;
import de.komoot.photon.importer.elasticsearch.Importer;
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

import static spark.Spark.get;
import static spark.Spark.setPort;

@Slf4j
public class App {
	public static void main(String[] rawArgs) {
		CommandLineArgs args = new CommandLineArgs();
		new JCommander(args, rawArgs);

		final Server esServer = new Server("photon", args.getDataDirectory());
		esServer.start();

		Client esNodeClient = esServer.getClient();

		if(args.isNominatimImport()) {
			esServer.recreateIndex(); // dump previous data
			Importer importer = new Importer(esServer, esNodeClient);
			NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
			nominatimConnector.setImporter(importer);
			nominatimConnector.readEntireDatabase();
		}

		if(args.isJsonDump()) {
			try {
				final JsonDumper jsonDumper = new JsonDumper("/tmp/photon_dump", args.getJsonLines());
				NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
				nominatimConnector.setImporter(jsonDumper);
				nominatimConnector.readEntireDatabase();
			} catch(FileNotFoundException e) {
				log.error("cannot create dump", e);
			}
		}

		if(args.getCreateSnapshot() != null) {
			esServer.createSnapshot(args.getCreateSnapshot());
		}

		if(args.isDeleteIndex()) {
			esServer.recreateIndex();
		}

		if(args.getImportSnapshot() != null) {
			esServer.deleteIndex();
			esServer.importSnapshot(args.getImportSnapshot());
		}

		final NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
		Updater updater = new ESUpdater(esNodeClient);
		nominatimUpdater.setUpdater(updater);

		setPort(2322);
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
				return "nominatim update started (more information in  console output) ...";
			}
		});

		get(new Route("api") {
			@Override
			public Object handle(Request request, Response response) {
				return "not yet implemented";
			}
		});
	}
}

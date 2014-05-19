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

		final Server esNode = new Server("photon", args.getDataDirectory());
		esNode.start();

		Client esNodeClient = esNode.getClient();

		if(args.isRecreate_index()) {
			Importer importer = new Importer(esNodeClient);
			importer.recreateIndex();
		}

		if(args.isIndexer()) {
			Importer importer = new Importer(esNodeClient);
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

		final NominatimUpdater nominatimUpdater = new NominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
		Updater updater = new ESUpdater(esNodeClient);
		nominatimUpdater.setUpdater(updater);
		setPort(2322);
		get(new Route("/", "text/html") {
			@Override
			public Object handle(Request request, Response response) {
				return "hallihallo";
			}
		});

		get(new Route("/bulk_import", "text/html") {
			@Override
			public Object handle(Request request, Response response) {
				return "hallihallo";
			}
		});

		get(new Route("/update", "text/html") {
			@Override
			public Object handle(Request request, Response response) {
				Thread nominatimUpdaterThread = new Thread() {
					@Override
					public void run() {
						nominatimUpdater.update();
					}
				};
				nominatimUpdaterThread.start();
				return "hallihallo";
			}
		});

		get(new Route("/create_dump", "text/html") {
			@Override
			public Object handle(Request request, Response response) {
				final String dumpName = (String) request.queryParams("name");
				Thread nominatimDumpThread = new Thread() {
					@Override
					public void run() {
						esNode.createSnapshot(dumpName);
					}
				};
				nominatimDumpThread.start();
				return "hallihallo";
			}
		});

		get(new Route("/import_dump", "text/html") {
			@Override
			public Object handle(Request request, Response response) {
				return "hallihallo";
			}
		});

		get(new Route("/geocode", "text/html") {
			@Override
			public Object handle(Request request, Response response) {
				return "hallihallo";
			}
		});

		get(new Route("/get_dump", "text/html") {
			@Override
			public Object handle(Request request, Response response) {
				return "hallihallo";
			}
		});

		//esNode.shutdown();
	}
}

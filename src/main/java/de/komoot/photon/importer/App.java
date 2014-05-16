package de.komoot.photon.importer;

import com.beust.jcommander.JCommander;
import de.komoot.photon.importer.elasticsearch.Server;
import de.komoot.photon.importer.nominatim.NominatimSource;
import de.komoot.photon.importer.nominatim.NominatimUpdater;
import de.komoot.photon.importer.elasticsearch.Importer;
import de.komoot.photon.importer.elasticsearch.Updater;
import org.elasticsearch.client.Client;
import spark.Request;
import spark.Response;
import spark.Route;

import static spark.Spark.get;

public class App {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {

		JCommanderStdin jct = new JCommanderStdin();
		new JCommander(jct, args);

		Server esNode = new Server("photon");
		esNode.start();

		Client esNodeClient = esNode.getClient();

		if(jct.isIndexer()) {
			Importer importer = new de.komoot.photon.importer.elasticsearch.Importer(esNodeClient);
			NominatimSource nominatimSource = new NominatimSource(jct.getHost(), jct.getPort(), jct.getDatabase(), jct.getUser(), jct.getPassword());
			nominatimSource.setImporter(importer);
			nominatimSource.export();
		}

        final NominatimUpdater nominatimUpdater = new NominatimUpdater(jct.getHost(), jct.getPort(), jct.getDatabase(), jct.getUser(), jct.getPassword());
        Updater updater = new Updater(esNodeClient);
        nominatimUpdater.setUpdater(updater);




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
                Thread nominatimUpdaterThread = new Thread(){
                    @Override
                    public void run(){
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

		//esNode.shutdown();
	}
}

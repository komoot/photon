package de.komoot.photon.importer;

import com.beust.jcommander.JCommander;
import de.komoot.photon.importer.elasticsearch.Server;
import de.komoot.photon.importer.nominatim.NominatimSource;
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

		get(new Route("/", "text/html") {
			@Override
			public Object handle(Request request, Response response) {
				return "hallihallo";
			}
		});

		//esNode.shutdown();
	}
}

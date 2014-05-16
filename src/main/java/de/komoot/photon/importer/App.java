package de.komoot.photon.importer;

import com.beust.jcommander.JCommander;
import de.komoot.photon.importer.elasticsearch.Server;
import de.komoot.photon.importer.nominatim.NominatimSource;
import org.elasticsearch.client.Client;

public class App {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {

		JCommanderStdin jct = new JCommanderStdin();
		new JCommander(jct, args);

		Server esNode = new Server("photon");
		esNode.start();

		Client esNodeClient = esNode.getClient();

		if(jct.isIndexer()) {
			NominatimSource nominatimSource = new NominatimSource(jct.getHost(), jct.getPort(), jct.getDatabase(), jct.getUser(), jct.getPassword());
			nominatimSource.export();
		}

		esNode.shutdown();
	}
}

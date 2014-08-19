package de.komoot.photon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import de.komoot.photon.importer.elasticsearch.Server;
import de.komoot.photon.importer.model.PhotonDoc;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * @author Peter Karich
 */
public class ESBaseTester {

	protected static Logger logger = LoggerFactory.getLogger(ESBaseTester.class);
	protected static int jettyPort;
	protected int resolved;
	private static Server server;
	public final static GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 5432);

	@AfterClass
	public static void tearDownClass() {
		shutdownES();
	}

	public void setUpES() {
		if(server != null)
			return;

		server = new Server(new File("./target/es_photon").getAbsolutePath()).start(true);
		server.recreateIndex();
	}

	protected Server getApiServer() {
		return server;
	}

	protected Client getClient() {
		if(server == null)
			throw new RuntimeException("call setUpES before using getClient");

		return server.getClient();
	}

	private final String indexName = "photon";

	protected void refresh() {
		getClient().admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
	}

	protected void deleteAll() {
		try {
			getClient().prepareDeleteByQuery(indexName).
					setQuery(QueryBuilders.matchAllQuery()).
					execute().actionGet();
		} catch(IndexMissingException ex) {
		}

		refresh();
	}

	public static void shutdownES() {
		if(server != null)
			server.shutdown();
	}

	public PhotonDoc createDoc(long placeId, String osmType, long osmId, Map<String, String> name, double lon, double lat) {
		final Point point = FACTORY.createPoint(new Coordinate(lon, lat));
		return new PhotonDoc(placeId, osmType, osmId, null, null, name, null, null, null, -1, 0.5, null, point, -1);
	}
}

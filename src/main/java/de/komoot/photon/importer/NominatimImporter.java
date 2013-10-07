package de.komoot.photon.importer;

import com.beust.jcommander.JCommander;
import de.komoot.photon.importer.model.CommandLineOptions;
import de.komoot.photon.importer.model.NominatimEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Entry point for the import of geo-spatial data from a nominatim database to convert it to a XML Solr file.
 *
 * @author christoph
 */
public class NominatimImporter {
	private final static Logger LOGGER = LoggerFactory.getLogger(NominatimImporter.class);
	private final IndexCrawler indexCrawler;
	private final Connection connection;
	private final File targetFile;
	private final List<String> languages;

	/**
	 * @param host       database host
	 * @param port       database port
	 * @param database   database name
	 * @param username   db username
	 * @param password   db username's password
	 * @param targetFile compressed output file (xml.gz)
	 * @param languages
	 */
	public NominatimImporter(String host, int port, String database, String username, String password, File targetFile, List<String> languages) {
		LOGGER.info(String.format("connecting to database [%s] host [%s] port [%d], output file: %s", database, host, port, targetFile.getPath()));

		this.targetFile = targetFile;
		this.languages = languages;
		try {
			Class.forName("org.postgresql.Driver");
		} catch(ClassNotFoundException e) {
			throw new RuntimeException("postgres driver cannot be loaded", e);
		}

		try {
			connection = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%d/%s", host, port, database), username, password);
			indexCrawler = new IndexCrawler(connection, languages);
		} catch(SQLException e) {
			throw new RuntimeException("cannot connect to database", e);
		}
	}

	/**
	 * starting reading nominatim database
	 *
	 * @param onlyBerlin
	 */
	public void run(boolean onlyBerlin) throws Exception {
		long startTime = System.currentTimeMillis();

		Exporter exporter = new XMLExporter(new FileOutputStream(targetFile));

		LOGGER.info("retrieving all items from nominatim database");
		ResultSet resultSet = indexCrawler.getAllRecords(onlyBerlin);
		long counter = 0;
		long time = System.currentTimeMillis();

		while(resultSet.next()) {
			if(counter % 10000 == 0 && counter > 0) {
				LOGGER.info(String.format("progress: %10d entries [%.1f / second]", counter, 10000000. / (1. * System.currentTimeMillis() - time)));
				time = System.currentTimeMillis();
			}

			NominatimEntry entry = new NominatimEntry(resultSet, languages);
			indexCrawler.completeInformation(entry);

			exporter.write(entry);

			counter++;
		}

		exporter.finish();

		long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
		double speed = counter * 1. / (elapsedSeconds + 0.01);
		LOGGER.info(String.format("finished %d entries after %d seconds [%.1f / second]", counter, elapsedSeconds, speed));
	}

	/**
	 * for testing purposes
	 *
	 * @param osmId
	 * @param type
	 * @return nominatim entry with all parents, null if osm_id could not be found
	 */
	public NominatimEntry getSingleEntry(long osmId, String type) throws SQLException {
		return indexCrawler.getSingleOSM(osmId, type);
	}

	public static void main(String[] args) throws Exception {
		CommandLineOptions options = new CommandLineOptions();
		new JCommander(options, args);

		NominatimImporter nominatimImporter = new NominatimImporter(options.host, options.port, options.database, options.username, options.password, options.outputFile, Collections.unmodifiableList(options.languages));
		nominatimImporter.run(options.onlyBerlin);
	}
}

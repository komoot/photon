package de.komoot.photon.importer.model;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * used for CommandLineOptions for JCommander
 *
 * @author christoph
 */
public class CommandLineOptions {
	@Parameter(names = "-h", description = "database host", required = true)
	public String host;

	@Parameter(names = "-d", description = "database name", required = true)
	public String database;

	@Parameter(names = "-u", description = "database username", required = true)
	public String username;

	@Parameter(names = "-P", description = "database password", required = false)
	public String password;

	@Parameter(names = "-p", description = "database port", required = false)
	public Integer port = 5432;

	@Parameter(names = "-b", description = "limit the import to extent of berlin", required = false)
	public boolean onlyBerlin = false;

	@Parameter(names = "-l", variableArity = true)
	public List<String> languages = new ArrayList<>();

	@Parameter(names = "-f", description = "file to save the output, e.g. europe.solr.data.xml.gz", required = true, converter = FileConverter.class)
	public File outputFile;
}

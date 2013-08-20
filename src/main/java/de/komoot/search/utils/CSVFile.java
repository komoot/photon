package de.komoot.search.utils;

import de.komoot.spring.utils.CSVReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * representation of a csv file with search tests
 */
public class CSVFile {
	private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CSVFile.class);
	private final CSVReader reader;
	private final List<CSVLine> lines = new ArrayList<CSVLine>();
	private final String name;

	List<String> requiredColumns = Arrays.asList("coordinate", "name");

	public CSVFile(CSVReader reader, String name) {
		this.reader = reader;
		this.name = name;
		readCases();
	}

	private void readCases() {
		while(true) {
			String[] line = null;
			try {
				line = this.reader.readline();
			} catch(IOException e) {
				break;
			}
			if(line == null) {
				// reached end of file
				break;
			}

			if(!matchesRequirement(line)) {
				continue;
			}

			CSVLine c = new CSVLine(reader.toMap(line));
			lines.add(c);
		}
	}

	private boolean matchesRequirement(String[] line) {

		Map<String, String> lineMap = reader.toMap(line);
		for(String key : requiredColumns) {
			if(lineMap.get(key) != null) {
				return true;
			}
		}

		LOGGER.warn(String.format("skipped invalid line required columns %s not found", requiredColumns));
		return false;
	}

	public List<CSVLine> getLines() {
		return lines;
	}

	public String getName() {
		return name;
	}
}

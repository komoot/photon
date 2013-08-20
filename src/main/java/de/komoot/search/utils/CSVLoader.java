package de.komoot.search.utils;

import de.komoot.spring.utils.CSVReader;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * helper class that provide access to all test cases (defined as csv) that were saved in a test directory
 * <p/>
 * User: christoph Date: 10.07.13
 */
public class CSVLoader {
	private List<CSVFile> tests;

	public CSVLoader(String path) throws URISyntaxException, IOException {
		URL resource = this.getClass().getResource(path);

		File file = new File(resource.toURI());
		if(file.isDirectory()) {
			tests = loadTests(file);
		} else {
			tests = Arrays.asList(loadTest(file));
		}
	}

	private CSVFile loadTest(File file) throws FileNotFoundException, IOException {
		CSVReader csvReader = new CSVReader(new FileInputStream(file));
		CSVFile test = new CSVFile(csvReader, file.getName());
		return test;
	}

	private List<CSVFile> loadTests(File directory) throws IOException, URISyntaxException {
		File[] csvFiles = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".csv");
			}
		});

		ArrayList<CSVFile> tests = new ArrayList<CSVFile>();
		for(File csvFile : csvFiles) {
			tests.add(loadTest(csvFile));
		}

		return tests;
	}

	/**
	 * get all tests defined in test folder
	 *
	 * @return
	 */
	public List<CSVFile> getTests() {
		return tests;
	}

	/**
	 * get the (first) test
	 *
	 * @return
	 */
	public CSVFile getTest() {
		return tests.get(0);
	}
}



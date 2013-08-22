package de.komoot.search.utils;

import org.springframework.util.Assert;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads a tab-separated csv resource.
 *
 * @author jan
 */
public class CSVReader implements Closeable {
	/**
	 * automatically generated Logger statement.
	 */
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CSVReader.class);
	private static final String EXCEPTION_INPUTSTREAM_IS_NULL = "inputstream is null";
	private BufferedReader reader;
	private String comment = null;
	private String[] headers;

	/**
	 * Creates a reader from the given classpath resource. The resource is read using UTF-8 as encoding. The first row is
	 * skipped.
	 *
	 * @param classpathresource a classpathresource (should be absolute)
	 * @throws IOException
	 */
	public CSVReader(String classpathresource) throws IOException {
		try(InputStream stream = getClass().getResourceAsStream(classpathresource)) {
			if(stream == null) {
				throw new IllegalArgumentException(classpathresource + " does not exist");
			}
			fromStream(stream, Charset.forName("UTF-8"), true);
		} finally {

		}
	}

	/**
	 * Creates a reader from the given InputStream. The first row is skipped.
	 *
	 * @param stream  a stream to read from
	 * @param charset charset of the stream
	 * @throws IOException
	 */
	public CSVReader(InputStream stream, Charset charset) throws IOException {
		Assert.notNull(stream, EXCEPTION_INPUTSTREAM_IS_NULL);
		fromStream(stream, charset, true);
	}

	/**
	 * Creates a reader from the given InputStream.
	 *
	 * @param stream           stream to read
	 * @param charset          charset of the stream
	 * @param firstRowAsHeader set to true if the first line contains the column headers
	 * @param comment          lines starting with this string are ignored
	 * @throws IOException
	 */
	public CSVReader(InputStream stream, Charset charset, boolean firstRowAsHeader, String comment) throws IOException {
		Assert.notNull(stream, EXCEPTION_INPUTSTREAM_IS_NULL);
		fromStream(stream, charset, firstRowAsHeader);
		this.comment = comment;
	}

	private void fromStream(InputStream stream, Charset charset, boolean firstRowAsHeader) throws IOException {
		this.reader = new BufferedReader(new InputStreamReader(stream, charset));
		if(firstRowAsHeader) {
			this.headers = readline();
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("Reading CSV files for columns: " + Arrays.toString(headers));
			}
		}
	}

	/**
	 * @return true if the internal {@link BufferedReader} is {@link BufferedReader#ready() ready}.
	 * @throws IOException
	 */
	public boolean ready() throws IOException {
		return reader.ready();
	}

	/**
	 * Reads the next line of the stream.
	 *
	 * @return the line or null if there is no line left
	 * @throws IOException
	 */
	public String[] readline() throws IOException {
		String line;
		while((line = reader.readLine()) != null) {
			if(comment != null && line.startsWith(comment)) {
				continue;
			} else {
				return line.split("\t");
			}
		}
		return null;
	}

	/**
	 * Reads lines while they have less than minCols columns. This method is useful to skip empty lines at the end of the
	 * line
	 *
	 * @param minCols minimum number of columns a line must have to be returned.
	 * @return an array of at least minCols columns or null if EOF.
	 * @throws IOException
	 */
	public String[] readline(int minCols) throws IOException {
		String[] s;
		while((s = readline()) != null) {
			if(s.length >= minCols) {
				return s;
			}
		}
		return null;
	}

	/**
	 * You can call this method if the first row of the source file contains column headers.
	 *
	 * @param line converts a previously retrieved line to a map.
	 * @return a Map from column name to the corresponding entry in <tt>line</tt>
	 */
	public Map<String, String> toMap(String[] line) {
		if(headers == null) {
			throw new IllegalStateException("First line not read as headers");
		} else {
			Map<String, String> map = new HashMap<String, String>(line.length);
			for(int i = 0; i < line.length && i < headers.length; i++) {
				map.put(headers[i], line[i]);
			}
			return map;
		}
	}

	@Override
	public void close() throws IOException {
		if(reader != null) {
			reader.close();
		}
	}
}

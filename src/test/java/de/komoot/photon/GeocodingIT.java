package de.komoot.photon;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.Math.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.elasticsearch.common.StopWatch;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Peter Karich
 */
public class GeocodingIT {

    private final OkHttpClient client = new OkHttpClient.Builder().
            connectTimeout(10, TimeUnit.SECONDS).
            readTimeout(10, TimeUnit.SECONDS).build();
    public final String itBase = "./src/test/resources/integration-test-data";
    public String serviceUrl = "http://photon.komoot.de/api?";

    @Before
    public void setUp() {
        serviceUrl = System.getProperty("api-url", serviceUrl);
    }

    @Test
    public void runWorld() {
        run(itBase + "/world", ".csv");
    }

    @Test
    public void runFrance() {
        run(itBase + "/world/france", ".csv");
    }

    @Test
    public void runFrance1() {
        run(itBase + "/world/france/iledefrance", ".csv");
    }

    @Test
    public void runFrance2() {
        run(itBase + "/world/france/nordpasdecalais", ".csv");
    }

    @Test
    public void runItaly() {
        run(itBase + "/world/italy", ".csv");
    }

    @Test
    public void runPoland() {
        run(itBase + "/world/poland", ".csv");
    }

    @Test
    public void runUSA() {
        run(itBase + "/world/usa", ".csv");
    }

    @Test
    public void runGermany() {
        run(itBase + "/world/germany", ".csv");
    }

    private List<File> getFiles(String dir, String ending) {
        return Arrays.asList(new File(dir).listFiles((File dir1, String name) -> {
            if (ending.isEmpty()) {
                return true;
            }
            return name.endsWith(ending);
        }));
    }

    private void run(String fileStr, String ending) {
        StopWatch sw = new StopWatch().start();
        List<Throwable> errors = new ArrayList<>();
        int requests = 0;
        List<File> files = getFiles(fileStr, ending);
        for (File file : files) {
            try {
                List<AssertQuery> queries = getQueries(Paths.get(file.toURI()));
                for (AssertQuery query : queries) {
                    requests++;
                    String url = query.createURL();

                    if (query instanceof SkipQuery) {
                        System.out.println("SKIP " + url);
                        errors.add(new RuntimeException("skipped " + url));
                        continue;
                    }

                    Response rsp = client.newCall(new Request.Builder().url(url).build()).execute();
                    Throwable th = query.assertResponse(rsp.body().string());
                    if (th != null) {
                        System.out.println("NOT OK " + url + " , expected:" + query.getExpected() + ", error: " + th.getMessage());
                        errors.add(th);
                    } else {
//                        System.out.println("query OK " + query);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        sw.stop();
        System.out.println("took " + sw.totalTime().toString() + " for " + requests + " requests");
        if (!errors.isEmpty()) {
            // TODO avoid too much details for now
//            for (Throwable th : errors) {
//                th.printStackTrace();
//            }
            throw new RuntimeException(errors.size() + " problems under " + requests + " requests occured for " + fileStr + " - see logs");
        }
    }

    private List<AssertQuery> getQueries(Path file) throws IOException {
        if (file.toString().endsWith(".csv")) {
            Map<String, Integer> headerMap;
            final String separator;

            try (Stream<String> lines = Files.lines(file)) {
                String firstLine = lines.findFirst().get();
                if (firstLine.contains(";")) {
                    separator = ";";
                } else {
                    separator = ",";
                }

                String header[] = firstLine.split(separator);
                headerMap = createHeaderMap(header);
            }

            if (headerMap.get("expected_coordinate") == null) {
                // TODO create new AssertQuery subclass that uses different expected values
                System.out.println("No 'expected_coordinate' column found. Cannot handle file " + file);
                return Collections.emptyList();
            }

            try (Stream<String> lines = Files.lines(file)) {
                return lines.
                        // skip header
                        skip(1).
                        // convert strings to object list
                        map((String t) -> {
                            String cols[] = t.split(separator);
                            Map<String, String> params = new HashMap<>();
                            params.put("q", getCol(cols, headerMap, "query"));
                            addParam(cols, headerMap, params, "lang");
                            addParam(cols, headerMap, params, "limit");
                            String expCoordinate = getCol(cols, headerMap, "expected_coordinate");
                            if (expCoordinate.isEmpty()) {
                                // filtering before is too much hassle
                                return new SkipQuery(serviceUrl, file.toString(), expCoordinate, params);
                            }
                            return new AssertQuery(serviceUrl, file.toString(), expCoordinate, params);
                        }).collect(toList());
            }
        } else {
            throw new UnsupportedOperationException("File type not supported " + file);
        }
    }

    private Map<String, Integer> createHeaderMap(String[] header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            if (header[i] != null) {
                String trimmed = header[i].trim();
                if (!trimmed.isEmpty()) {
                    map.put(trimmed, i);
                }
            }
        }
        return map;
    }

    String getCol(String[] cols, Map<String, Integer> map, String key) {
        Integer colIdx = map.get(key);
        if (colIdx == null) {
            throw new RuntimeException("Cannot find column '" + key + "' in map:" + map + ", columns:" + Arrays.toString(cols));
        }
        if (colIdx >= cols.length) {
            throw new RuntimeException("Too large column index " + colIdx + ">=" + cols.length + " for key " + key);
        }
        String colVal = cols[colIdx];
        // remove quotation marks
        if (colVal.startsWith("\"") && colVal.endsWith("\"")) {
            return colVal.substring(1, colVal.length() - 1).trim();
        }
        return colVal.trim();
    }

    private void addParam(String[] cols, Map<String, Integer> headerMap, Map<String, String> params, String key) {
        if (headerMap.get(key) != null) {
            String val = getCol(cols, headerMap, key);
            if (!val.isEmpty()) {
                params.put(key, val);
            }
        }
    }

    public static class SkipQuery extends AssertQuery {

        public SkipQuery(String baseUrl, String file, String expectedString, Map<String, String> params) {
            super(baseUrl, file, params);
        }
    }

    public static class AssertQuery {

        private final String file;
        private String queryString;
        final double[] expectedLonLat;
        final double expectedRadius;
        final String baseUrl;

        protected AssertQuery(String baseUrl, String file, Map<String, String> params) {
            this.baseUrl = baseUrl;
            this.file = file;
            this.expectedRadius = -1;
            this.expectedLonLat = new double[]{};
            this.queryString = params.toString();
        }

        public AssertQuery(String baseUrl, String file, String expectedString, Map<String, String> params) {
            this.baseUrl = baseUrl;
            this.file = file;
            this.queryString = "";
            try {
                for (Entry<String, String> entry : params.entrySet()) {
                    queryString += "&" + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
                }
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
            String strings[] = expectedString.split(",");
            if (strings.length >= 2) {
                this.expectedLonLat = new double[]{Double.parseDouble(strings[1]), Double.parseDouble(strings[0])};
            } else {
                throw new RuntimeException("expected coordinates wrong '" + expectedString + "' in " + file);
            }

            if (strings.length >= 3) {
                this.expectedRadius = Double.parseDouble(strings[2]);
            } else {
                this.expectedRadius = 100;
            }
        }

        public String createURL() {
            return baseUrl + queryString;
        }

        private Throwable assertResponse(String response) {
            double lon, lat;
            try {
                JSONArray lonlat = new JSONObject(response).getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                lon = lonlat.getDouble(0);
                lat = lonlat.getDouble(1);
                // GH API
//                JSONObject lonlat = new JSONObject(response).getJSONArray("hits").getJSONObject(0).getJSONObject("point");
//                lon = lonlat.getDouble("lng");
//                lat = lonlat.getDouble("lat");

            } catch (Exception ex) {
                return new RuntimeException("Wrong response format " + response);
            }
            double dist = calcDist(expectedLonLat[1], expectedLonLat[0], lat, lon);
            if (dist > expectedRadius) {
                return new RuntimeException("Problematic query '" + queryString + "' distance " + (float) dist + " too far away (expected <" + expectedRadius + "). File " + file);
            }
            return null;
        }

        @Override
        public String toString() {
            return queryString;
        }

        private String getExpected() {
            return "lonlat:" + Arrays.toString(expectedLonLat) + ", radius:" + expectedRadius;
        }
    }

    public static double calcDist(double fromLat, double fromLon, double toLat, double toLon) {
        double sinDeltaLat = sin(toRadians(toLat - fromLat) / 2);
        double sinDeltaLon = sin(toRadians(toLon - fromLon) / 2);
        double normalizedDist = sinDeltaLat * sinDeltaLat
                + sinDeltaLon * sinDeltaLon * cos(toRadians(fromLat)) * cos(toRadians(toLat));
        return 6371000 * 2 * asin(sqrt(normalizedDist));
    }
}

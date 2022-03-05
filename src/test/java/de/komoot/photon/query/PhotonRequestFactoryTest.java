package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;

import static org.junit.jupiter.api.Assertions.*;

import de.komoot.photon.searcher.TagFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import spark.QueryParamsMap;
import spark.Request;

import java.util.*;
import java.util.stream.Stream;

/**
 * Tests for correct parsing of the query parameters into a PhotonRequest.
 */
public class PhotonRequestFactoryTest {

    private PhotonRequest create(String... queryParams) throws BadRequestException {
        Request mockRequest = Mockito.mock(Request.class);

        Set<String> keys = new HashSet<>();
        for (int pos = 0; pos < queryParams.length; pos += 2) {
            Mockito.when(mockRequest.queryParams(queryParams[pos])).thenReturn(queryParams[pos + 1]);
            keys.add(queryParams[pos]);
        }

        Mockito.when(mockRequest.queryParams()).thenReturn(keys);

        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);

        PhotonRequestFactory factory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        return factory.create(mockRequest);
    }

    private PhotonRequest createOsmFilters(String... filterParams) throws BadRequestException {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("new york");

        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockQueryParamsMap.hasValue()).thenReturn(true);
        Mockito.when(mockQueryParamsMap.values()).thenReturn(filterParams);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);

        PhotonRequestFactory factory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        return factory.create(mockRequest);
    }

    @Test
    public void testWithLocationBiasAndLimit() throws Exception {
        PhotonRequest photonRequest = create("q", "berlin", "lon", "-87", "lat", "41", "limit", "5");

        assertAll("request",
                () -> assertEquals("berlin", photonRequest.getQuery()),
                () -> assertEquals(-87, photonRequest.getLocationForBias().getX(), 0),
                () -> assertEquals(41, photonRequest.getLocationForBias().getY(), 0),
                () -> assertEquals(5, photonRequest.getLimit())
        );
    }

    @Test
    public void testWithEmptyLimit() throws Exception {
        PhotonRequest photonRequest = create("q", "berlin", "limit", "");

        assertEquals(15, photonRequest.getLimit());
    }

    @Test
    public void testWithoutLocationBias() throws Exception {
        PhotonRequest photonRequest = create("q", "berlin");

        assertAll("request",
                () -> assertEquals("berlin", photonRequest.getQuery()),
                () -> assertNull(photonRequest.getLocationForBias())
        );
    }

    @Test
    public void testInfiniteScale() throws Exception {
        PhotonRequest photonRequest = create("q", "berlin", "location_bias_scale", "Infinity");

        assertEquals(1.0, photonRequest.getScaleForBias());
    }

    @Test
    public void testEmptyScale() throws Exception {
        PhotonRequest photonRequest = create("q", "berlin", "location_bias_scale", "");

        assertEquals(0.2, photonRequest.getScaleForBias());
    }

    @Test
    public void testWithDebug() throws Exception {
        PhotonRequest photonRequest = create("q", "berlin", "debug", "1");

        assertEquals(true, photonRequest.getDebug());
    }

    @ParameterizedTest
    @MethodSource("badParamsProvider")
    public void testBadParameters(List<String> queryParams) {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> create(queryParams.toArray(new String[0])));
    }

    static Stream<List<String>> badParamsProvider() {
        return Stream.of(
                Arrays.asList("q", "nowhere", "extra", "data"), // unknown parameter
                Arrays.asList("q", "berlin", "limit", "x"), // limit that is not a number
                Arrays.asList("q", "berlin", "location_bias_scale", "-e"), // score that is not a number
                Arrays.asList("q", "berlin", "location_bias_scale", "NaN"), // score with NaN
                Arrays.asList("q", "berlin", "lon", "3", "lat", "bad"), // bad latitude parameter
                Arrays.asList("q", "berlin", "lon", "bad", "lat", "45"), // bad longitude parameter
                Arrays.asList("lat", "45", "lon", "45"),  // missing query parameter
                Arrays.asList("q", "hanover", "bbox", "9.6,52.3,9.8") , // bbox, wrong number of inputs
                Arrays.asList("q", "hanover", "bbox", "9.6,52.3,NaN,9.8") , // bbox, bad parameter (NaN)
                Arrays.asList("q", "hanover", "bbox", "9.6,52.3,-Infinity,9.8") , // bbox, bad parameter (Inf)
                Arrays.asList("q", "hanover", "bbox", "9.6,52.3,r34,9.8") , // bbox, bad parameter (garbage)
                Arrays.asList("q", "hanover", "bbox", "9.6,-92,9.8,14"), // bbox, min lat 90
                Arrays.asList("q", "hanover", "bbox", "9.6,14,9.8,91"), // bbox, max lat 90
                Arrays.asList("q", "hanover", "bbox", "-181, 9, 4, 12"), // bbox, min lon 180
                Arrays.asList("q", "hanover", "bbox", "12, 9, 181, 12") // bbox, max lon 180
        );
    }

    @Test
    public void testTagFilters() throws Exception {
        PhotonRequest photonRequest = createOsmFilters("foo", ":!bar");

        List<TagFilter> result = photonRequest.getOsmTagFilters();

        assertEquals(2, result.size());

        assertAll("filterlist",
                () -> assertNotNull(result.get(0)),
                () -> assertNotNull(result.get(1))
        );
    }

    @Test
    public void testBadTagFilters() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> createOsmFilters("good", "bad:bad:bad"));
    }

    @Test
    public void testWithBboxFilter() throws Exception {
        PhotonRequest photonRequest = create("q", "hanover", "bbox", "9.6,52.3,9.8,52.4");

        assertEquals(new Envelope(9.6, 9.8, 52.3, 52.4), photonRequest.getBbox());
    }
}
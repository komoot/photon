package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;
import de.komoot.photon.searcher.TagFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import spark.QueryParamsMap;
import spark.Request;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for correct parsing of the query parameters into a PhotonRequest.
 */
public class PhotonRequestFactoryTest {

    private Request createRequestWithQueryParams(String... queryParams) throws BadRequestException {
        Request mockRequest = Mockito.mock(Request.class);

        Set<String> keys = new HashSet<>();
        for (int pos = 0; pos < queryParams.length; pos += 2) {
            Mockito.when(mockRequest.queryParams(queryParams[pos])).thenReturn(queryParams[pos + 1]);
            keys.add(queryParams[pos]);
        }

        Mockito.when(mockRequest.queryParams()).thenReturn(keys);

        QueryParamsMap mockEmptyQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockEmptyQueryParamsMap);
        Mockito.when(mockRequest.queryMap("layer")).thenReturn(mockEmptyQueryParamsMap);

        return mockRequest;
    }

    public void requestWithOsmFilters(Request mockRequest, String... filterParams) {
        Mockito.when(mockRequest.queryParams("q")).thenReturn("new york");

        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockQueryParamsMap.hasValue()).thenReturn(true);
        Mockito.when(mockQueryParamsMap.values()).thenReturn(filterParams);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
    }

    public void requestWithLayers(Request mockRequest, String... layers) {
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockQueryParamsMap.hasValue()).thenReturn(true);
        Mockito.when(mockQueryParamsMap.values()).thenReturn(layers);
        Mockito.when(mockRequest.queryMap("layer")).thenReturn(mockQueryParamsMap);
    }

    private PhotonRequest createPhotonRequest(Request mockRequest) throws BadRequestException {
        PhotonRequestFactory factory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        return factory.create(mockRequest);
    }

    @Test
    public void testWithLocationBiasAndLimit() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "berlin", "lon", "-87", "lat", "41", "limit", "5");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        assertAll("request",
                () -> assertEquals("berlin", photonRequest.getQuery()),
                () -> assertEquals(-87, photonRequest.getLocationForBias().getX(), 0),
                () -> assertEquals(41, photonRequest.getLocationForBias().getY(), 0),
                () -> assertEquals(5, photonRequest.getLimit())
        );
    }

    @Test
    public void testWithEmptyLimit() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "berlin", "limit", "");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        assertEquals(15, photonRequest.getLimit());
    }

    @Test
    public void testWithoutLocationBias() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "berlin");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        assertAll("request",
                () -> assertEquals("berlin", photonRequest.getQuery()),
                () -> assertNull(photonRequest.getLocationForBias())
        );
    }

    @Test
    public void testInfiniteScale() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "berlin", "location_bias_scale", "Infinity");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        assertEquals(1.0, photonRequest.getScaleForBias());
    }

    @Test
    public void testEmptyScale() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "berlin", "location_bias_scale", "");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        assertEquals(0.2, photonRequest.getScaleForBias());
    }

    @Test
    public void testWithDebug() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "berlin", "debug", "1");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        assertEquals(true, photonRequest.getDebug());
    }

    @ParameterizedTest
    @MethodSource("badParamsProvider")
    public void testBadParameters(List<String> queryParams, String expectedMessageFragment) throws BadRequestException {
        Request mockRequest = createRequestWithQueryParams(queryParams.toArray(new String[0]));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> createPhotonRequest(mockRequest));
        assertTrue(exception.getMessage().contains(expectedMessageFragment),
                   String.format("Error message doesn not contain '%s': %s", expectedMessageFragment, exception.getMessage()));
    }

    static Stream<Arguments> badParamsProvider() {
        return Stream.of(
                arguments(Arrays.asList("q", "nowhere", "extra", "data"), "'extra'"), // unknown parameter
                arguments(Arrays.asList("q", "berlin", "limit", "x"), "'limit'"), // limit that is not a number
                arguments(Arrays.asList("q", "berlin", "location_bias_scale", "-e"), "'location_bias_scale'"), // score that is not a number
                arguments(Arrays.asList("q", "berlin", "location_bias_scale", "NaN"), "'location_bias_scale'"), // score with NaN
                arguments(Arrays.asList("q", "berlin", "lon", "3", "lat", "bad"), "'lat'"), // bad latitude parameter
                arguments(Arrays.asList("q", "berlin", "lon", "bad", "lat", "45"), "'lon'"), // bad longitude parameter
                arguments(Arrays.asList("lat", "45", "lon", "45"), "'q'"),  // missing query parameter
                arguments(Arrays.asList("q", "hanover", "bbox", "9.6,52.3,9.8"), "'bbox'"), // bbox, wrong number of inputs
                arguments(Arrays.asList("q", "hanover", "bbox", "9.6,52.3,NaN,9.8"), "'bbox'"), // bbox, bad parameter (NaN)
                arguments(Arrays.asList("q", "hanover", "bbox", "9.6,52.3,-Infinity,9.8"), "'bbox'"), // bbox, bad parameter (Inf)
                arguments(Arrays.asList("q", "hanover", "bbox", "9.6,52.3,r34,9.8"), "'bbox'"), // bbox, bad parameter (garbage)
                arguments(Arrays.asList("q", "hanover", "bbox", "9.6,-92,9.8,14"), "'bbox'"), // bbox, min lat 90
                arguments(Arrays.asList("q", "hanover", "bbox", "9.6,14,9.8,91"), "'bbox'"), // bbox, max lat 90
                arguments(Arrays.asList("q", "hanover", "bbox", "-181, 9, 4, 12"), "'bbox'"), // bbox, min lon 180
                arguments(Arrays.asList("q", "hanover", "bbox", "12, 9, 181, 12"), "'bbox'") // bbox, max lon 180
        );
    }

    @Test
    public void testTagFilters() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "new york");
        requestWithOsmFilters(mockRequest, "foo", ":!bar");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        List<TagFilter> result = photonRequest.getOsmTagFilters();

        assertEquals(2, result.size());

        assertAll("filterlist",
                () -> assertNotNull(result.get(0)),
                () -> assertNotNull(result.get(1))
        );
    }

    @Test
    public void testBadTagFilters() throws BadRequestException {
        Request mockRequest = createRequestWithQueryParams("q", "new york");
        requestWithOsmFilters(mockRequest, "good", "bad:bad:bad");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> createPhotonRequest(mockRequest));
    }

    @Test
    public void testWithBboxFilter() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "hanover", "bbox", "9.6,52.3,9.8,52.4");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        assertEquals(new Envelope(9.6, 9.8, 52.3, 52.4), photonRequest.getBbox());
    }

    @Test
    public void testWithLayerFilters() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "new york");
        requestWithLayers(mockRequest, "city", "locality");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        assertEquals(new HashSet<>(Arrays.asList("city", "locality")), photonRequest.getLayerFilters());
    }

    @Test
    public void testWithDuplicatedLayerFilters() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "new york");
        requestWithLayers(mockRequest, "city", "locality", "city");
        PhotonRequest photonRequest = createPhotonRequest(mockRequest);

        assertEquals(new HashSet<>(Arrays.asList("city", "locality")), photonRequest.getLayerFilters());
    }

    @Test
    public void testWithBadLayerFilters() throws Exception {
        Request mockRequest = createRequestWithQueryParams("q", "new york");
        requestWithLayers(mockRequest, "city", "bad");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> createPhotonRequest(mockRequest));

        String expectedMessageFragment = "layer";

        assertTrue(exception.getMessage().contains(expectedMessageFragment),
                String.format("Error message doesn not contain '%s': %s", expectedMessageFragment, exception.getMessage()));
    }
}
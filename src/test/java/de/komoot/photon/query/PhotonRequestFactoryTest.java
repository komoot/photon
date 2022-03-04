package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;

import static org.junit.jupiter.api.Assertions.*;

import de.komoot.photon.searcher.TagFilter;
import de.komoot.photon.searcher.TagFilterKind;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import spark.QueryParamsMap;
import spark.Request;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonRequestFactoryTest {

    private PhotonRequest photonRequest;

    @Test
    public void testWithLocationBiasAndLimit() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        Mockito.when(mockRequest.queryParams("lon")).thenReturn("-87");
        Mockito.when(mockRequest.queryParams("lat")).thenReturn("41");
        Mockito.when(mockRequest.queryParams("limit")).thenReturn("5");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        photonRequest = photonRequestFactory.create(mockRequest);
        assertEquals("berlin", photonRequest.getQuery());
        assertEquals(-87, photonRequest.getLocationForBias().getX(), 0);
        assertEquals(41, photonRequest.getLocationForBias().getY(), 0);
        assertEquals(5, photonRequest.getLimit());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lat");
    }

    @Test
    public void testWithoutLocationBias() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        Mockito.when(mockRequest.queryParams("lon")).thenReturn(null);
        Mockito.when(mockRequest.queryParams("lat")).thenReturn(null);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        photonRequest = photonRequestFactory.create(mockRequest);
        assertEquals("berlin", photonRequest.getQuery());
        assertNull(photonRequest.getLocationForBias());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lat");
    }

    @Test
    public void testWithBadBias() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        Mockito.when(mockRequest.queryParams("lon")).thenReturn("bad");
        Mockito.when(mockRequest.queryParams("lat")).thenReturn("bad");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        
        try {
            PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
            photonRequest = photonRequestFactory.create(mockRequest);
            fail();
        } catch (BadRequestException e) {
            assertEquals("invalid search term 'lat' and/or 'lon', try instead lat=51.5&lon=8.0", e.getMessage());
        }
        
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lat");
    }

    @Test
    public void testWithBadLimit() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        Mockito.when(mockRequest.queryParams("limit")).thenReturn(null);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        photonRequest = photonRequestFactory.create(mockRequest);
        assertEquals("berlin", photonRequest.getQuery());
        assertEquals(new Integer(15), photonRequest.getLimit());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("limit");
    }

    @Test
    public void testWithBadQuery() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn(null);
        try {
            PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
            photonRequest = photonRequestFactory.create(mockRequest);
            fail();
        } catch (BadRequestException e) {
            assertEquals("missing search term 'q': /?q=berlin", e.getMessage());
        }
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
    }

    @Test
    public void testWithIncludeKeyFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{"aTag"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(1)).values();

        assertEquals(Collections.singletonList(new TagFilter(TagFilterKind.INCLUDE, "aTag", null)), filteredPhotonRequest.getOsmTagFilters());
    }

    @Test
    public void testWithIncludeTagFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{"aTag:aValue"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(1)).values();

        assertEquals(Collections.singletonList(new TagFilter(TagFilterKind.INCLUDE, "aTag", "aValue")), filteredPhotonRequest.getOsmTagFilters());
    }

    @Test
    public void testWithIncludeValueFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{":aValue"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(1)).values();

        assertEquals(Collections.singletonList(new TagFilter(TagFilterKind.INCLUDE, null, "aValue")), filteredPhotonRequest.getOsmTagFilters());
    }

    @Test
    public void testWithExcludeKeyFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{"!aTag"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(1)).values();

        assertEquals(Collections.singletonList(new TagFilter(TagFilterKind.EXCLUDE, "aTag", null)), filteredPhotonRequest.getOsmTagFilters());
    }

    @Test
    public void testWithExcludeTagFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{"!aTag:aValue"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(1)).values();

        assertEquals(Collections.singletonList(new TagFilter(TagFilterKind.EXCLUDE, "aTag", "aValue")), filteredPhotonRequest.getOsmTagFilters());
    }

    @Test
    public void testWithExcludeValueFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{"!:aValue"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(1)).values();

        assertEquals(Collections.singletonList(new TagFilter(TagFilterKind.EXCLUDE, null, "aValue")), filteredPhotonRequest.getOsmTagFilters());
    }
    
    @Test
    public void testWithBboxFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("hanover");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        Mockito.when(mockRequest.queryParams("bbox")).thenReturn("9.6,52.3,9.8,52.4");
        
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
        PhotonRequest photonRequest = photonRequestFactory.create(mockRequest); 
        
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("bbox");
        assertEquals(new Envelope(9.6, 9.8, 52.3, 52.4), photonRequest.getBbox());
    }

    @Test
    public void testWithBboxFilterWrongNumberOfInputs() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("hanover");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        Mockito.when(mockRequest.queryParams("bbox")).thenReturn("9.6,52.3,9.8");

        try {
            PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
            photonRequestFactory.create(mockRequest);
            fail();
        } catch (BadRequestException e) {
            assertEquals(BoundingBoxParamConverter.INVALID_BBOX_ERROR_MESSAGE, e.getMessage());
        }
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("bbox");
    }
    
    @Test
    public void testWithBadBboxFilterMinLat90() throws Exception {
        testBoundingBoxResponse(9.6,-92,9.8,14);
    }

    @Test
    public void testWithBadBboxFilterMaxLat90() throws Exception {
        testBoundingBoxResponse(9.6,14,9.8,91);
    }

    @Test
    public void testWithBadBboxFilterMinLon180() throws Exception {
        testBoundingBoxResponse(-181, 9, 4, 12);
    }

    @Test
    public void testWithBadBboxFilterMaxLon180() throws Exception {
        testBoundingBoxResponse(12, 9, 181, 12);
    }


    public void testBoundingBoxResponse(double minLon, double minLat, double maxLon, double maxLat) {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("hanover");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        Mockito.when(mockRequest.queryParams("bbox")).thenReturn(minLon + "," + minLat + "," + maxLon + "," + maxLat);

        try {
            PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(Collections.singletonList("en"), "en");
            photonRequestFactory.create(mockRequest);
            fail();
        } catch (BadRequestException e) {
            assertEquals(BoundingBoxParamConverter.INVALID_BBOX_BOUNDS_MESSAGE, e.getMessage());
        }
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("bbox");
    }
}
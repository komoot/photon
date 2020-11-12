package de.komoot.photon.query;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Envelope;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import spark.QueryParamsMap;
import spark.Request;

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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        photonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", photonRequest.getQuery());
        Assert.assertEquals(-87, photonRequest.getLocationForBias().getX(), 0);
        Assert.assertEquals(41, photonRequest.getLocationForBias().getY(), 0);
        Assert.assertEquals(new Integer(5), photonRequest.getLimit());
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        photonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", photonRequest.getQuery());
        Assert.assertNull(photonRequest.getLocationForBias());
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
            PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
            photonRequest = photonRequestFactory.create(mockRequest);
            Assert.fail();
        } catch (BadRequestException e) {
            Assert.assertEquals("invalid search term 'lat' and/or 'lon', try instead lat=51.5&lon=8.0", e.getMessage());
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        photonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", photonRequest.getQuery());
        Assert.assertEquals(new Integer(15), photonRequest.getLimit());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("limit");
    }

    @Test
    public void testWithBadQuery() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn(null);
        try {
            PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
            photonRequest = photonRequestFactory.create(mockRequest);
            Assert.fail();
        } catch (BadRequestException e) {
            Assert.assertEquals("missing search term 'q': /?q=berlin", e.getMessage());
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(2)).values();
        Assert.assertEquals(ImmutableSet.of("aTag"), filteredPhotonRequest.keys());
    }

    @Test
    public void testWithIncludeTagFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{"aTag:aValue"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(2)).values();
        Assert.assertEquals(ImmutableMap.of("aTag", ImmutableSet.of("aValue")), filteredPhotonRequest.tags());
    }

    @Test
    public void testWithIncludeValueFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{":aValue"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(2)).values();
        Assert.assertEquals(ImmutableSet.of("aValue"), filteredPhotonRequest.values());
    }

    @Test
    public void testWithExcludeKeyFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{"!aTag"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(2)).values();
        Assert.assertEquals(ImmutableSet.of("aTag"), filteredPhotonRequest.notKeys());
    }

    @Test
    public void testWithExcludeTagFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{"!aTag:aValue"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(2)).values();
        Assert.assertEquals(ImmutableMap.of("aTag", ImmutableSet.of("aValue")), filteredPhotonRequest.notTags());
    }

    @Test
    public void testWithExcludeValueFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        QueryParamsMap mockOsmTagQueryParm = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockOsmTagQueryParm.values()).thenReturn(new String[]{"!:aValue"});
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockOsmTagQueryParm);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        PhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(2)).values();
        Assert.assertEquals(ImmutableSet.of("aValue"), filteredPhotonRequest.notValues());
    }
    
    @Test
    public void testWithBboxFilter() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("hanover");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        Mockito.when(mockRequest.queryParams("bbox")).thenReturn("9.6,52.3,9.8,52.4");
        
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
        PhotonRequest photonRequest = photonRequestFactory.create(mockRequest); 
        
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("bbox");
        Assert.assertEquals(new Envelope(9.6, 9.8, 52.3, 52.4), photonRequest.getBbox());
    }

    @Test
    public void testWithBboxFilterWrongNumberOfInputs() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("hanover");
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        Mockito.when(mockRequest.queryParams("bbox")).thenReturn("9.6,52.3,9.8");

        try {
            PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
            photonRequestFactory.create(mockRequest);
            Assert.fail();
        } catch (BadRequestException e) {
            Assert.assertEquals(BoundingBoxParamConverter.INVALID_BBOX_ERROR_MESSAGE, e.getMessage());
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
            PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableList.of("en"), "en");
            photonRequestFactory.create(mockRequest);
            Assert.fail();
        } catch (BadRequestException e) {
            Assert.assertEquals(BoundingBoxParamConverter.INVALID_BBOX_BOUNDS_MESSAGE, e.getMessage());
        }
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("bbox");
    }
}
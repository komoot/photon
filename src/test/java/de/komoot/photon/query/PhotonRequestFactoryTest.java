package de.komoot.photon.query;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        photonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", photonRequest.getQuery());
        Assert.assertNull(photonRequest.getLocationForBias());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.never()).queryParams("lat");
    }

    @Test
    public void testWithBadBias() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        Mockito.when(mockRequest.queryParams("lon")).thenReturn("bad");
        Mockito.when(mockRequest.queryParams("lat")).thenReturn("bad");
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
        QueryParamsMap mockQueryParamsMap = Mockito.mock(QueryParamsMap.class);
        Mockito.when(mockRequest.queryMap("osm_tag")).thenReturn(mockQueryParamsMap);
        photonRequest = photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", photonRequest.getQuery());
        Assert.assertNull(photonRequest.getLocationForBias());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.never()).queryParams("lat");
    }

    @Test
    public void testWithBadLimit() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        Mockito.when(mockRequest.queryParams("limit")).thenReturn(null);
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
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
            PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        FilteredPhotonRequest filteredPhotonRequest = (FilteredPhotonRequest) photonRequestFactory.create(mockRequest);
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        FilteredPhotonRequest filteredPhotonRequest = (FilteredPhotonRequest) photonRequestFactory.create(mockRequest);
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
        FilteredPhotonRequest filteredPhotonRequest = photonRequestFactory.create(mockRequest);
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        FilteredPhotonRequest filteredPhotonRequest = (FilteredPhotonRequest) photonRequestFactory.create(mockRequest);
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        FilteredPhotonRequest filteredPhotonRequest = (FilteredPhotonRequest) photonRequestFactory.create(mockRequest);
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
        PhotonRequestFactory photonRequestFactory = new PhotonRequestFactory(ImmutableSet.of("en"));
        Mockito.when(mockOsmTagQueryParm.hasValue()).thenReturn(true);
        FilteredPhotonRequest filteredPhotonRequest = (FilteredPhotonRequest) photonRequestFactory.create(mockRequest);
        Assert.assertEquals("berlin", filteredPhotonRequest.getQuery());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryMap("osm_tag");
        Mockito.verify(mockOsmTagQueryParm, Mockito.times(2)).values();
        Assert.assertEquals(ImmutableSet.of("aValue"), filteredPhotonRequest.notValues());
    }
}
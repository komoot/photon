package de.komoot.photon;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import spark.Request;

public class PhotonRequestTest {

    private PhotonRequest photonRequest;

    @Test
    public void testWithLocationBiasAndLimit() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        Mockito.when(mockRequest.queryParams("lon")).thenReturn("-87");
        Mockito.when(mockRequest.queryParams("lat")).thenReturn("41");
        Mockito.when(mockRequest.queryParams("limit")).thenReturn("5");
        photonRequest = new PhotonRequest(mockRequest);
        Assert.assertEquals("berlin", photonRequest.getQuery());
        Assert.assertEquals(-87, photonRequest.getLocationBias().getLon(), 0);
        Assert.assertEquals(41, photonRequest.getLocationBias().getLat(), 0);
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
        photonRequest = new PhotonRequest(mockRequest);
        Assert.assertEquals("berlin", photonRequest.getQuery());
        Assert.assertNull(photonRequest.getLocationBias());
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
        photonRequest = new PhotonRequest(mockRequest);
        Assert.assertEquals("berlin", photonRequest.getQuery());
        Assert.assertNull(photonRequest.getLocationBias());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.never()).queryParams("lat");
    }

    @Test
    public void testWithBadLimit() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn("berlin");
        Mockito.when(mockRequest.queryParams("limit")).thenReturn(null);
        photonRequest = new PhotonRequest(mockRequest);
        Assert.assertEquals("berlin", photonRequest.getQuery());
        Assert.assertNull(photonRequest.getLocationBias());
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("limit");
    } 
    @Test
    public void testWithBadQuery() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("q")).thenReturn(null);
        try {
            photonRequest = new PhotonRequest(mockRequest);
            Assert.fail();
        } catch (BadRequestException e) {
            Assert.assertEquals("missing search term 'q': /?q=berlin",e.getMessage());
        }
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("q");
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.komoot.photon.query;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import spark.Request;

/**
 *
 * @author svantulden
 */
public class ReverseRequestFactoryTest {

    private ReverseRequest reverseRequest;

    @Test
    public void testWithLocation() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("lon")).thenReturn("-87");
        Mockito.when(mockRequest.queryParams("lat")).thenReturn("41");
        ReverseRequestFactory reverseRequestFactory = new ReverseRequestFactory(ImmutableSet.of("en"));
        reverseRequest = reverseRequestFactory.create(mockRequest);
        Assert.assertEquals(-87, reverseRequest.getLocation().getX(), 0);
        Assert.assertEquals(41, reverseRequest.getLocation().getY(), 0);
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lat");
    }

    @Test
    public void testWithBadLocation() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("lon")).thenReturn("bad");
        Mockito.when(mockRequest.queryParams("lat")).thenReturn("bad");
        try {
            ReverseRequestFactory reverseRequestFactory = new ReverseRequestFactory(ImmutableSet.of("en"));
            reverseRequest = reverseRequestFactory.create(mockRequest);
            Assert.fail();
        } catch (BadRequestException e) {
            Assert.assertEquals("missing search term 'lat' and/or 'lon': /?lat=51.5&lon=8.0", e.getMessage());
        }
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.never()).queryParams("lat");
    }

    @Test
    public void testWithNoLocation() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("lon")).thenReturn(null);
        Mockito.when(mockRequest.queryParams("lat")).thenReturn(null);
        try {
            ReverseRequestFactory reverseRequestFactory = new ReverseRequestFactory(ImmutableSet.of("en"));
            reverseRequest = reverseRequestFactory.create(mockRequest);
            Assert.fail();
        } catch (BadRequestException e) {
            Assert.assertEquals("missing search term 'lat' and/or 'lon': /?lat=51.5&lon=8.0", e.getMessage());
        }
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.never()).queryParams("lat");
    }
}
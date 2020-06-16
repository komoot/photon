/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.komoot.photon.query;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import spark.Request;

/**
 * @author svantulden
 */
public class ReverseRequestFactoryTest {

    private ReverseRequest reverseRequest;

    public void requestWithLongitudeLatitude(Request mockRequest, Double longitude, Double latitude) {
        Mockito.when(mockRequest.queryParams("lon")).thenReturn(longitude.toString());
        Mockito.when(mockRequest.queryParams("lat")).thenReturn(latitude.toString());
        Mockito.when(mockRequest.queryParamOrDefault("distance_sort", "true")).thenReturn("true");
    }

    @Test
    public void testWithLocation() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        requestWithLongitudeLatitude(mockRequest, -87d, 41d);
        ReverseRequestFactory reverseRequestFactory = new ReverseRequestFactory(ImmutableList.of("en"), "en");
        reverseRequest = reverseRequestFactory.create(mockRequest);
        Assert.assertEquals(-87, reverseRequest.getLocation().getX(), 0);
        Assert.assertEquals(41, reverseRequest.getLocation().getY(), 0);
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lat");
    }

    public void assertBadRequest(Request mockRequest, String expectedMessage) {
        try {
            ReverseRequestFactory reverseRequestFactory = new ReverseRequestFactory(ImmutableList.of("en"), "en");
            reverseRequest = reverseRequestFactory.create(mockRequest);
            Assert.fail();
        } catch (BadRequestException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testWithBadLocation() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("lon")).thenReturn("bad");
        Mockito.when(mockRequest.queryParams("lat")).thenReturn("bad");
        assertBadRequest(mockRequest, "invalid search term 'lat' and/or 'lon', try instead lat=51.5&lon=8.0");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lat");
    }


    public void testWithHighLowLongitude(Boolean high) throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        requestWithLongitudeLatitude(mockRequest, (high) ? 180.01 : -180.01, 0.0);
        assertBadRequest(mockRequest, "invalid search term 'lon', expected number >= -180.0 and <= 180.0");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lat");
    }

    @Test
    public void testWithHighLongitude() throws Exception {
        testWithHighLowLongitude(true);
    }

    @Test
    public void testWithLowLongitude() throws Exception {
        testWithHighLowLongitude(false);
    }

    public void testWithHighLowLatitude(Boolean high) throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        requestWithLongitudeLatitude(mockRequest, 0.0, (high) ? 90.01 : -90.01);
        assertBadRequest(mockRequest, "invalid search term 'lat', expected number >= -90.0 and <= 90.0");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lat");
    }

    @Test
    public void testWithHighLatitude() throws Exception {
        testWithHighLowLatitude(true);
    }

    @Test
    public void testWithLowLatitude() throws Exception {
        testWithHighLowLatitude(false);
    }

    @Test
    public void testWithNoLocation() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        Mockito.when(mockRequest.queryParams("lon")).thenReturn("");
        Mockito.when(mockRequest.queryParams("lat")).thenReturn("");
        assertBadRequest(mockRequest, "invalid search term 'lat' and/or 'lon', try instead lat=51.5&lon=8.0");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lon");
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("lat");
    }

    public void testWithBadParam(String paramName, String paramValue, String expectedMessage) throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        requestWithLongitudeLatitude(mockRequest, -87d, 41d);
        Mockito.when(mockRequest.queryParams(paramName)).thenReturn(paramValue);
        assertBadRequest(mockRequest, expectedMessage);
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams(paramName);
    }

    @Test
    public void testWithNegativeRadius() throws Exception {
        testWithBadParam("radius", "-10.0", "invalid search term 'radius', expected a strictly positive number.");
    }

    @Test
    public void testWithZeroRadius() throws Exception {
        testWithBadParam("radius", "0.0", "invalid search term 'radius', expected a strictly positive number.");
    }

    @Test
    public void testWithBadRadius() throws Exception {
        testWithBadParam("radius", "bad", "invalid search term 'radius', expected a number.");
    }

    @Test
    public void testHighRadius() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        requestWithLongitudeLatitude(mockRequest, -87d, 41d);
        Mockito.when(mockRequest.queryParams("radius")).thenReturn("5.1");
        ReverseRequestFactory reverseRequestFactory = new ReverseRequestFactory(ImmutableList.of("en"), "en");
        reverseRequest = reverseRequestFactory.create(mockRequest);
        Assert.assertEquals(reverseRequest.getRadius(), 5.1d, 0);
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("radius");
    }

    @Test
    public void testWithNegativeLimit() throws Exception {
        testWithBadParam("limit", "-1", "invalid search term 'limit', expected a strictly positive integer.");
    }

    @Test
    public void testWithZeroLimit() throws Exception {
        testWithBadParam("limit", "0", "invalid search term 'limit', expected a strictly positive integer.");
    }

    @Test
    public void testWithBadLimit() throws Exception {
        testWithBadParam("limit", "bad", "invalid search term 'limit', expected an integer.");
    }

    @Test
    public void testHighLimit() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        requestWithLongitudeLatitude(mockRequest, -87d, 41d);
        Mockito.when(mockRequest.queryParams("limit")).thenReturn("51");
        ReverseRequestFactory reverseRequestFactory = new ReverseRequestFactory(ImmutableList.of("en"), "en");
        reverseRequest = reverseRequestFactory.create(mockRequest);
        Assert.assertEquals(reverseRequest.getLimit().longValue(), 50);
        Mockito.verify(mockRequest, Mockito.times(1)).queryParams("limit");
    }
    
    @Test
    public void testDistanceSortDefault() throws Exception {
        Request mockRequest = Mockito.mock(Request.class);
        requestWithLongitudeLatitude(mockRequest, -87d, 41d);
        ReverseRequestFactory reverseRequestFactory = new ReverseRequestFactory(ImmutableList.of("en"), "en");
        reverseRequest = reverseRequestFactory.create(mockRequest);
        Mockito.verify(mockRequest, Mockito.times(1)).queryParamOrDefault("distance_sort", "true");
        Assert.assertEquals(true, reverseRequest.getLocationDistanceSort());
    }
}
package de.komoot.photon;

import com.google.common.collect.ImmutableSet;
import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import spark.Request;
import spark.Response;
import spark.Route;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class SearchRequestHandlerTest {
    @Test
    public void testConstructor() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        SearchRequestHandler searchRequestHandler = new SearchRequestHandler("any", "en,fr");
        String path = ReflectionTestUtil.getFieldValue(searchRequestHandler, Route.class, "path");
        Assert.assertEquals("any", path);
        Set<String> langauges = ReflectionTestUtil.getFieldValue(searchRequestHandler, searchRequestHandler.getClass(), "supportedLanguages");
        Assert.assertEquals(ImmutableSet.of("en", "fr"), langauges);
        PhotonSearcherFactory searchFactory = ReflectionTestUtil.getFieldValue(searchRequestHandler, searchRequestHandler.getClass(), "searcherFactory");
        Assert.assertNotNull(searchFactory);
    }

    @Test
    public void testHandle() throws BadRequestException {
        SearchRequestHandler searchRequestHandler = new SearchRequestHandler("any", "en,fr");
        Response mockResponse = Mockito.mock(Response.class);
        Request mockRequest = Mockito.mock(Request.class);
        PhotonRequestFactory mockPhotonRequestFactory = Mockito.mock(PhotonRequestFactory.class);
        PhotonRequest mockPhotonRequest = Mockito.mock(PhotonRequest.class);
        Mockito.when(mockPhotonRequestFactory.create(mockRequest)).thenReturn(mockPhotonRequest);
        ReflectionTestUtil.setFieldValue(searchRequestHandler,SearchRequestHandler.class,"photonRequestFactory",mockPhotonRequestFactory);
        PhotonRequestHandler mockPhotonRequestHandler = Mockito.mock(PhotonRequestHandler.class);
        Mockito.when(mockPhotonRequestHandler.handle(mockPhotonRequest)).thenReturn("success");
        ReflectionTestUtil.setFieldValue(searchRequestHandler,SearchRequestHandler.class,"photonRequestHandler",mockPhotonRequestHandler);
        String resultString = searchRequestHandler.handle(mockRequest, mockResponse);
        Assert.assertFalse(resultString.startsWith("bad request"));
        Assert.assertEquals("success",resultString);
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.komoot.photon;

import com.google.common.collect.ImmutableSet;
import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.LanguageChecker;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.ReverseRequestFactory;
import de.komoot.photon.searcher.ReverseRequestHandlerFactory;
import de.komoot.photon.searcher.SimpleReverseRequestHandler;
import de.komoot.photon.utils.ConvertToGeoJson;
import org.hamcrest.core.IsEqual;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import spark.Request;
import spark.Response;
import spark.Route;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import org.elasticsearch.client.Client;
import spark.RouteImpl;

/**
 *
 * @author svantulden
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SimpleReverseRequestHandler.class)
public class ReverseSearchRequestHandlerTest {
    @Test
    public void testConstructor() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Client client = Mockito.mock(Client.class);
        ReverseSearchRequestHandler reverseSearchRequestHandler = new ReverseSearchRequestHandler("any", client, "en,fr");
        String path = ReflectionTestUtil.getFieldValue(reverseSearchRequestHandler, RouteImpl.class, "path");
        Assert.assertEquals("any", path);
        ReverseRequestFactory reverseRequestFactory = ReflectionTestUtil.getFieldValue(reverseSearchRequestHandler, reverseSearchRequestHandler.getClass(), "reverseRequestFactory");
        LanguageChecker languageChecker = ReflectionTestUtil.getFieldValue(reverseRequestFactory, reverseRequestFactory.getClass(), "languageChecker");
        Set<String> supportedLanguages = ReflectionTestUtil.getFieldValue(languageChecker, languageChecker.getClass(), "supportedLanguages");
        Set<String> supportedLanguagesExpected = ImmutableSet.of("en", "fr");
        Assert.assertThat(supportedLanguages, IsEqual.equalTo(supportedLanguagesExpected));
    }

    @Test
    public void testHandle() throws BadRequestException {
        Client client = Mockito.mock(Client.class);
        ReverseSearchRequestHandler reverseSearchRequestHandlerUnderTest = new ReverseSearchRequestHandler("any", client, "en,fr");
        ReverseRequestFactory mockReverseRequestFactory = Mockito.mock(ReverseRequestFactory.class);
        Request mockWebRequest = Mockito.mock(Request.class);
        ReflectionTestUtil.setFieldValue(reverseSearchRequestHandlerUnderTest, ReverseSearchRequestHandler.class, "reverseRequestFactory", mockReverseRequestFactory);
        ReverseRequestHandlerFactory mockReverseRequestHandlerFactory = Mockito.mock(ReverseRequestHandlerFactory.class);
        ReflectionTestUtil.setFieldValue(reverseSearchRequestHandlerUnderTest, ReverseSearchRequestHandler.class, "requestHandlerFactory", mockReverseRequestHandlerFactory);
        SimpleReverseRequestHandler mockSimpleReverseRequestHandler = PowerMockito.mock(SimpleReverseRequestHandler.class);
        PowerMockito.when(mockSimpleReverseRequestHandler.handle(Mockito.any(ReverseRequest.class))).thenReturn(new ArrayList<JSONObject>());
        Mockito.when(mockReverseRequestHandlerFactory.createHandler(Mockito.any(ReverseRequest.class))).thenReturn(mockSimpleReverseRequestHandler);
        ConvertToGeoJson mockConvertToGeoJson = Mockito.mock(ConvertToGeoJson.class);
        ReflectionTestUtil.setFieldValue(reverseSearchRequestHandlerUnderTest, "geoJsonConverter", mockConvertToGeoJson);
        String expectedResultString = "{\"test\":\"success\"}";
        Mockito.when(mockConvertToGeoJson.doForward(Mockito.any(ArrayList.class))).thenReturn(new JSONObject(expectedResultString));
        String finalResult = reverseSearchRequestHandlerUnderTest.handle(mockWebRequest, Mockito.mock(Response.class));
        Assert.assertThat(finalResult, IsEqual.equalTo(expectedResultString));
    }
}
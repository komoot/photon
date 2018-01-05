package de.komoot.photon;

import com.google.common.collect.ImmutableSet;
import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.LanguageChecker;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.searcher.PhotonRequestHandlerFactory;
import de.komoot.photon.searcher.SimplePhotonRequestHandler;
import de.komoot.photon.utils.ConvertToGeoJson;
import org.elasticsearch.client.Client;
import org.hamcrest.core.IsEqual;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SimplePhotonRequestHandler.class)
@PowerMockIgnore({"javax.management.*"})
public class SearchRequestHandlerTest {
    @Test
    public void testConstructor() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Client client = Mockito.mock(Client.class);
        SearchRequestHandler searchRequestHandler = new SearchRequestHandler("any", client, "en,fr");
        String path = ReflectionTestUtil.getFieldValue(searchRequestHandler, RouteImpl.class, "path");
        Assert.assertEquals("any", path);
        PhotonRequestFactory photonRequestFactory = ReflectionTestUtil.getFieldValue(searchRequestHandler, searchRequestHandler.getClass(), "photonRequestFactory");
        LanguageChecker languageChecker = ReflectionTestUtil.getFieldValue(photonRequestFactory, photonRequestFactory.getClass(), "languageChecker");
        Set<String> supportedLanguages = ReflectionTestUtil.getFieldValue(languageChecker, languageChecker.getClass(), "supportedLanguages");
        Set<String> supportedLanguagesExpected = ImmutableSet.of("en", "fr");
        Assert.assertThat(supportedLanguages, IsEqual.equalTo(supportedLanguagesExpected));
    }

    @Test
    public void testHandle() throws BadRequestException {
        Client client = Mockito.mock(Client.class);
        SearchRequestHandler searchRequestHandlerUnderTest = new SearchRequestHandler("any", client, "en,fr");
        PhotonRequestFactory mockPhotonRequestFactory = Mockito.mock(PhotonRequestFactory.class);
        Request mockWebRequest = Mockito.mock(Request.class);
        ReflectionTestUtil.setFieldValue(searchRequestHandlerUnderTest, SearchRequestHandler.class, "photonRequestFactory", mockPhotonRequestFactory);
        PhotonRequestHandlerFactory mockPhotonRequestHandlerFactory = Mockito.mock(PhotonRequestHandlerFactory.class);
        ReflectionTestUtil.setFieldValue(searchRequestHandlerUnderTest, SearchRequestHandler.class, "requestHandlerFactory", mockPhotonRequestHandlerFactory);
        SimplePhotonRequestHandler mockSimplePhotonRequestHandler = PowerMockito.mock(SimplePhotonRequestHandler.class);
        PowerMockito.when(mockSimplePhotonRequestHandler.handle(Mockito.any(PhotonRequest.class))).thenReturn(new ArrayList<JSONObject>());
        Mockito.when(mockPhotonRequestHandlerFactory.createHandler(Mockito.any(PhotonRequest.class))).thenReturn(mockSimplePhotonRequestHandler);
        ConvertToGeoJson mockConvertToGeoJson = Mockito.mock(ConvertToGeoJson.class);
        ReflectionTestUtil.setFieldValue(searchRequestHandlerUnderTest, "geoJsonConverter", mockConvertToGeoJson);
        String expectedResultString = "{\"test\":\"success\"}";
        Mockito.when(mockConvertToGeoJson.doForward(Mockito.any(ArrayList.class))).thenReturn(new JSONObject(expectedResultString));
        String finalResult = searchRequestHandlerUnderTest.handle(mockWebRequest, Mockito.mock(Response.class));
        Assert.assertThat(finalResult, IsEqual.equalTo(expectedResultString));
    }
}
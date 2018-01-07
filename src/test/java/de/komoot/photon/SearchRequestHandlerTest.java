package de.komoot.photon;

import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.LanguageChecker;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.searcher.PhotonRequestHandler;
import de.komoot.photon.searcher.PhotonRequestHandlerFactory;
import de.komoot.photon.searcher.SimplePhotonRequestHandler;
import de.komoot.photon.utils.ConvertToGeoJson;
import org.elasticsearch.client.Client;
import org.hamcrest.core.IsEqual;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        SimplePhotonRequestHandler mockSimplePhotonRequestHandler = new SimplePhotonRequestHandler(null) {
            public List<JSONObject> handle(PhotonRequest photonRequest) {
                return new ArrayList<>();
            }
        };
        PhotonRequestHandlerFactory mockPhotonRequestHandlerFactory = new PhotonRequestHandlerFactory(null) {
            @Override
            public PhotonRequestHandler<PhotonRequest> createHandler(PhotonRequest request) {
                return mockSimplePhotonRequestHandler;
            }
        };
        ReflectionTestUtil.setFieldValue(searchRequestHandlerUnderTest, SearchRequestHandler.class, "requestHandlerFactory", mockPhotonRequestHandlerFactory);

        ConvertToGeoJson mockConvertToGeoJson = Mockito.mock(ConvertToGeoJson.class);
        ReflectionTestUtil.setFieldValue(searchRequestHandlerUnderTest, "geoJsonConverter", mockConvertToGeoJson);
        String expectedResultString = "{\"test\":\"success\"}";
        Mockito.when(mockConvertToGeoJson.doForward(Mockito.any(ArrayList.class))).thenReturn(new JSONObject(expectedResultString));
        String finalResult = searchRequestHandlerUnderTest.handle(mockWebRequest, Mockito.mock(Response.class));
        Assert.assertThat(finalResult, IsEqual.equalTo(expectedResultString));
    }
}
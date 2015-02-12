package de.komoot.photon;

import com.google.common.collect.ImmutableSet;
import de.komoot.photon.query.PhotonRequestFactory;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import spark.Route;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class SearchRequestHandlerTest {
    @Test
    public void testConstructor() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        SearchRequestHandler  searchRequestHandler= new SearchRequestHandler("any","en,fr");
        String path = ReflectionTestUtil.getFieldValue(searchRequestHandler, Route.class, "path");
        Assert.assertEquals("any", path);
        Set<String> langauges = ReflectionTestUtil.getFieldValue(searchRequestHandler,searchRequestHandler.getClass(),"supportedLanguages");
        Assert.assertEquals(ImmutableSet.of("en","fr"),langauges);
        PhotonSearcherFactory searchFactory = ReflectionTestUtil.getFieldValue(searchRequestHandler,searchRequestHandler.getClass(),"searcherFactory");
        Assert.assertNotNull(searchFactory);
    }
    
    @Test
    public void testHandle(){
        PhotonRequestFactory mockPhotonRequestFactory = Mockito.mock(PhotonRequestFactory.class);
//        Mockito.when(mockPhotonRequestFactory.create(Mockito.any(Request.class))).thenReturn(new BasePhotonRequest());

    }
}
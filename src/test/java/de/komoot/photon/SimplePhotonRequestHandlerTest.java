package de.komoot.photon;

import de.komoot.photon.query.FilteredPhotonRequest;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.searcher.PhotonSearcher;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class SimplePhotonRequestHandlerTest {

    @Test
    public void testHandleSearchStrict() throws Exception {
        PhotonSearcherFactory mockPhotonSearcherFactory = Mockito.mock(PhotonSearcherFactory.class);
        ReflectionTestUtil.setFieldValue(new SimplePhotonRequestHandler(), SimplePhotonRequestHandler.class, "searcherFactory", mockPhotonSearcherFactory);
        PhotonSearcher mockPhotonSearcher = Mockito.mock(PhotonSearcher.class);
        Mockito.when(mockPhotonSearcher.searchStrict()).thenReturn(Arrays.asList(new JSONObject()));
        Mockito.when(mockPhotonSearcherFactory.getSearcher(Mockito.any(PhotonRequest.class))).thenReturn(mockPhotonSearcher);
        Mockito.verify(mockPhotonSearcherFactory).getSearcher(Mockito.any(FilteredPhotonRequest.class));
        Mockito.verify(mockPhotonSearcher).searchStrict();
    }
}
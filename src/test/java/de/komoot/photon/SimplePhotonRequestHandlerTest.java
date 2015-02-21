package de.komoot.photon;

import de.komoot.photon.searcher.PhotonSearcher;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.junit.Test;
import org.mockito.Mockito;

public class SimplePhotonRequestHandlerTest {

    @Test
    public void testHandleSearchStrict() throws Exception {
        PhotonSearcherFactory mockPhotonSearcherFactory = Mockito.mock(PhotonSearcherFactory.class);
        ReflectionTestUtil.setFieldValue(new SimplePhotonRequestHandler(), SimplePhotonRequestHandler.class, "searcherFactory", mockPhotonSearcherFactory);
        PhotonSearcher mockPhotonSearcher = Mockito.mock(PhotonSearcher.class);
//        Mockito.when(mockPhotonSearcher.searchStrict(queryBuilder, photonRequest.getLimit())).thenReturn(Arrays.asList(new JSONObject()));
//        Mockito.when(mockPhotonSearcherFactory.getSearcher(Mockito.any(PhotonRequest.class))).thenReturn(mockPhotonSearcher);
//        Mockito.verify(mockPhotonSearcherFactory).getSearcher(Mockito.any(FilteredPhotonRequest.class));
//        Mockito.verify(mockPhotonSearcher).searchStrict(queryBuilder, photonRequest.getLimit());
    }
}
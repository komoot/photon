package de.komoot.photon;

import de.komoot.photon.query.FilteredPhotonRequest;
import de.komoot.photon.searcher.PhotonSearcherFactory;
import org.junit.Test;
import org.mockito.Mockito;

public class SimplePhotonRequestHandlerTest {

    @Test
    public void testHandle() throws Exception {
        PhotonSearcherFactory mockPhotonSearcherFactory = Mockito.mock(PhotonSearcherFactory.class);
        Mockito.verify(mockPhotonSearcherFactory).getSearcher(Mockito.any(FilteredPhotonRequest.class));

    }
}
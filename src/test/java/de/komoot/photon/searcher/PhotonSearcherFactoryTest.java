package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class PhotonSearcherFactoryTest {

    @Test
    public void testGetSearcher() throws Exception {
        PhotonSearcherFactory photonSearcherFactory = new PhotonSearcherFactory();
        PhotonRequest mockPhotonRequest = Mockito.mock(PhotonRequest.class);
        PhotonSearcher searcher = photonSearcherFactory.getSearcher(mockPhotonRequest);
        Assert.assertNull(searcher);
    }
}
package de.komoot.photon.searcher;

import com.google.common.collect.ImmutableSet;
import de.komoot.photon.query.FilteredPhotonRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class PhotonSearcherFactoryTest {

    @Test
    public void testGetSearcher() throws Exception {
        PhotonSearcherFactory photonSearcherFactory = new PhotonSearcherFactory();
        FilteredPhotonRequest mockPhotonRequest = Mockito.mock(FilteredPhotonRequest.class);
        Mockito.when(mockPhotonRequest.getQuery()).thenReturn("berlin");
        Mockito.when(mockPhotonRequest.getLimit()).thenReturn(15);
        Mockito.when(mockPhotonRequest.keys()).thenReturn(ImmutableSet.of("aTag"));
        PhotonSearcher searcher = photonSearcherFactory.getSearcher(mockPhotonRequest);
        Assert.assertEquals(BasePhotonSearcher.class,searcher.getClass());
    }
}
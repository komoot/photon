package de.komoot.photon.searcher;

import com.google.common.collect.ImmutableSet;
import de.komoot.photon.query.FilteredPhotonRequest;
import org.elasticsearch.client.Client;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ElasticsearchSearcherFactoryTest {

    @Test
    public void testGetSearcher() throws Exception {
        Client client = Mockito.mock(Client.class);
        ElasticsearchSearcherFactory elasticsearchSearcherFactory = new ElasticsearchSearcherFactory(client);
        FilteredPhotonRequest mockPhotonRequest = Mockito.mock(FilteredPhotonRequest.class);
        Mockito.when(mockPhotonRequest.getQuery()).thenReturn("berlin");
        Mockito.when(mockPhotonRequest.getLimit()).thenReturn(15);
        Mockito.when(mockPhotonRequest.keys()).thenReturn(ImmutableSet.of("aTag"));
        ElasticsearchSearcher searcher = elasticsearchSearcherFactory.getSearcher(mockPhotonRequest);
        Assert.assertEquals(BaseElasticsearchSearcher.class,searcher.getClass());
    }
}

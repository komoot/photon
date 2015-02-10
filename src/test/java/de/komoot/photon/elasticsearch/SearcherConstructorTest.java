package de.komoot.photon.elasticsearch;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.Client;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;

public class SearcherConstructorTest {

    @Test
    public void testConstructorForSearchDslTemplates() throws NoSuchFieldException, IllegalAccessException, IOException {
        Client mockClient = Mockito.mock(Client.class);
        Searcher searcher = new Searcher(mockClient);
        checkQueries(searcher,"query.json","queryTemplate");
        checkQueries(searcher,"query_location_bias.json","queryLocationBiasTemplate");
        checkQueries(searcher, "query_tag_key_value_filter.json","queryWithTagKeyValueFiltersTemplate");
        checkQueries(searcher, "query_tag_key_value_filter_location_bias.json","queryWithTagKeyValueFiltersAndBiasTemplate");
        checkQueries(searcher, "query_tag_key_filter.json","queryWithTagKeyFiltersTemplate");
        checkQueries(searcher, "query_tag_key_filter_location_bias.json","queryWithTagKeyFiltersAndBiasTemplate");
    }

    private void checkQueries(Searcher searcher, String fileNameForExpectedJson, String fieldNameInSearcher) throws NoSuchFieldException, IOException, IllegalAccessException {
        {
            Field queryTemplateField = Searcher.class.getDeclaredField(fieldNameInSearcher);
            queryTemplateField.setAccessible(true);
            Object queryString = queryTemplateField.get(searcher);
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            String expected = IOUtils.toString(loader.getResourceAsStream(fileNameForExpectedJson), "UTF-8");
            Assert.assertEquals(expected, queryString);
        }

    }


}
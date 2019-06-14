package de.komoot.photon.query;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Set;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import de.komoot.photon.ESBaseTester;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class TagFilterQueryBuilderSearchTest extends ESBaseTester {

    /**
     * Find me all places named "berlin" that are tagged "tourism=attraction"
     *
     * @throws IOException
     */
    @Test
    public void testFilterWithTagTourismAttraction() throws IOException {
        Client client = getClient();
        Set<String> valueSet = ImmutableSet.of("attraction");
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withTags(ImmutableMap.of("tourism", valueSet));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(2l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged with a value of "attraction".
     */
    @Test
    public void testValueAttraction() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withValues("attraction");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(2l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged with key "tourism".
     */
    @Test
    public void testKeyTourism() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withKeys("tourism");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(8l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are NOT tagged "tourism=attraction"
     */
    @Test
    public void testFilterWithoutTagTourismAttraction() throws IOException {
        Client client = getClient();
        Set<String> valueSet = ImmutableSet.of("attraction");
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withoutTags(ImmutableMap.of("tourism", valueSet));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(16l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that do not have the value "information" in their tags - no matter what key
     */
    @Test
    public void testValueNotInformation() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withoutValues("information");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(12l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that do not have the key "tourism" in their tags
     */
    @Test
    public void testKeyNotTourism() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withoutKeys("tourism");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(10l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not tagged with value "information".
     * <p/>
     * Note: This is a different method of achieving the same result as
     * {@link TagFilterQueryBuilderSearchTest#testKeyTourismButValueNotInformation()}
     */
    @Test
    public void testKeyTourismAndValueNotInformation() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withKeys("tourism").withoutValues("information");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(6l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not tagged with value "information".
     * <p/>
     * Note: This is a different method of achieving the same result as
     * {@link TagFilterQueryBuilderSearchTest#testKeyTourismAndValueNotInformation}.
     */
    @Test
    public void testKeyTourismButValueNotInformation() throws IOException {
        Client client = getClient();
        Set<String> valueSet = ImmutableSet.of("information");
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withTagsNotValues(ImmutableMap.of("tourism", valueSet));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(6l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged without the keys "tourism" and "amenity".
     */
    @Test
    public void testKeyNotTourismAndKeyNotAmenity() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withoutKeys("tourism", "amenity");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(4l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not "amenity". This test works, but,
     * the use case does not make sense because by searching for key "tourism", this test already excludes places keyed
     * on "amenity"
     */
    @Test
    public void testKeyTourismAndKeyNotAmenity() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withKeys("tourism").withoutKeys("amenity");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(8l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that have value "information" but not key "amenity"
     */
    @Test
    public void testValueInformationButKeyNotAmenity() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withValues("information").withoutKeys("amenity");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(4l));

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that do not have the tag tourism=attraction
     */
    @Test
    public void testTagNotTourismAttraction() throws IOException {
        Client client = getClient();
        Set<String> attraction = ImmutableSet.of("attraction");
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en").withoutTags(ImmutableMap.of("tourism", attraction));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertThat(searchResponse.getHits().getTotalHits(), is(16l));

        deleteIndex();
    }

    private SearchResponse search(Client client, QueryBuilder queryBuilder) {
        return client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
    }
}

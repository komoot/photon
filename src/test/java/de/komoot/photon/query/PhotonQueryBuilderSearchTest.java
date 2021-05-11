package de.komoot.photon.query;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.elasticsearch.Importer;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Before;
import org.junit.Test;


/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class PhotonQueryBuilderSearchTest extends ESBaseTester {

    @Before
    public void setUp() throws Exception {
        setUpES();
        ImmutableList<String> tags = ImmutableList.of("tourism", "attraction", "tourism", "hotel", "tourism", "museum", "tourism", "information", "amenity",
                "parking", "amenity", "restaurant", "amenity", "information", "food", "information", "railway", "station");
        Importer instance = new Importer(getClient(), "en", "");
        double lon = 13.38886;
        double lat = 52.51704;
        for (int i = 0; i < tags.size(); i++) {
            String key = tags.get(i);
            String value = tags.get(++i);
            PhotonDoc doc = this.createDoc(lon, lat, i, i, key, value);
            instance.add(doc);
            lon += 0.00004;
            lat += 0.00086;
            doc = this.createDoc(lon, lat, i + 1, i + 1, key, value);
            instance.add(doc);
            lon += 0.00004;
            lat += 0.00086;
        }
        instance.finish();
        refresh();
    }


    /**
     * Find me all places named "berlin" that are tagged "tourism=attraction"
     *
     * @throws IOException
     */
    @Test
    public void testFilterWithTagTourismAttraction() throws IOException {
        Client client = getClient();
        Set<String> valueSet = ImmutableSet.of("attraction");
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withTags(ImmutableMap.of("tourism", valueSet));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(2l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged with a value of "attraction".
     */
    @Test
    public void testValueAttraction() throws IOException {
        Client client = getClient();
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withValues("attraction");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(2l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged with key "tourism".
     */
    @Test
    public void testKeyTourism() throws IOException {
        Client client = getClient();
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withKeys("tourism");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(8l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are NOT tagged "tourism=attraction"
     */
    @Test
    public void testFilterWithoutTagTourismAttraction() throws IOException {
        Client client = getClient();
        Set<String> valueSet = ImmutableSet.of("attraction");
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withoutTags(ImmutableMap.of("tourism", valueSet));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(16l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that do not have the value "information" in their tags - no matter what key
     */
    @Test
    public void testValueNotInformation() throws IOException {
        Client client = getClient();
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withoutValues("information");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(12l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that do not have the key "tourism" in their tags
     */
    @Test
    public void testKeyNotTourism() throws IOException {
        Client client = getClient();
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withoutKeys("tourism");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(10l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not tagged with value "information".
     * <p/>
     * Note: This is a different method of achieving the same result as
     * {@link PhotonQueryBuilderSearchTest#testKeyTourismButValueNotInformation()}
     */
    @Test
    public void testKeyTourismAndValueNotInformation() throws IOException {
        Client client = getClient();
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withKeys("tourism").withoutValues("information");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(6l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not tagged with value "information".
     * <p/>
     * Note: This is a different method of achieving the same result as
     * {@link PhotonQueryBuilderSearchTest#testKeyTourismAndValueNotInformation}.
     */
    @Test
    public void testKeyTourismButValueNotInformation() throws IOException {
        Client client = getClient();
        Set<String> valueSet = ImmutableSet.of("information");
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withTagsNotValues(ImmutableMap.of("tourism", valueSet));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(6l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that are tagged without the keys "tourism" and "amenity".
     */
    @Test
    public void testKeyNotTourismAndKeyNotAmenity() throws IOException {
        Client client = getClient();
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withoutKeys("tourism", "amenity");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(4l, searchResponse.getHits().getTotalHits());

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
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withKeys("tourism").withoutKeys("amenity");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(8l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that have value "information" but not key "amenity"
     */
    @Test
    public void testValueInformationButKeyNotAmenity() throws IOException {
        Client client = getClient();
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withValues("information").withoutKeys("amenity");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(4l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    /**
     * Find me all places named "berlin" that do not have the tag tourism=attraction
     */
    @Test
    public void testTagNotTourismAttraction() throws IOException {
        Client client = getClient();
        Set<String> attraction = ImmutableSet.of("attraction");
        PhotonQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true).withoutTags(ImmutableMap.of("tourism", attraction));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = search(client, queryBuilder);
        assertEquals(16l, searchResponse.getHits().getTotalHits());

        deleteIndex();
    }

    private SearchResponse search(Client client, QueryBuilder queryBuilder) {
        return client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
    }
}

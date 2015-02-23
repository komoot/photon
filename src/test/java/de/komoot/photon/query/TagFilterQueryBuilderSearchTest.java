package de.komoot.photon.query;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.elasticsearch.Importer;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class TagFilterQueryBuilderSearchTest extends ESBaseTester {
    GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private Client client;
    private List<PhotonDoc> testData;


    @Before
    public void setUp() throws IOException {
        setUpES();
        deleteAll();
        ImmutableList<String> tags = ImmutableList.of("tourism", "attraction",
                                                      "tourism", "hotel",
                                                      "tourism", "museum",
                                                      "tourism", "information",
                                                      "amenity", "parking",
                                                      "amenity", "restaurant",
                                                      "amenity", "information",
                                                      "railway", "station");
        Importer instance = new Importer(getClient(), "en");
        for (int i = 0; i < tags.size(); i++) {
            String key = tags.get(i);
            String value = tags.get(++i);
            PhotonDoc doc = this.createDoc(i, key, value);
            instance.add(doc);
            doc = this.createDoc(i + 1, key, value);
            instance.add(doc);
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
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withTags(ImmutableMap.of("tourism", valueSet));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(2l));
    }

    /**
     * Find me all places named "berlin" that are tagged "tourism" with any value.
     *
     * @throws IOException
     */
    @Test
    public void testKeyTourism() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withKeys("tourism");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(8l));
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not tagged with value "information". This method of including a key and excluding a value *
     * separately may exclude another key that has the same value. See {@link TagFilterQueryBuilderSearchTest#testKeyTourismValueNotInformationAnotherWay()}
     *
     * @throws IOException
     */
    @Test
    public void testKeyTourismValueNotInformation() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withKeys("tourism").withoutValues("information");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(6l));
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not tagged with value "information". This is similar to 
     * {@link TagFilterQueryBuilderSearchTest#testKeyTourismValueNotInformation} but, does it explicitly for one key instead of excluding all values regardless of key. This method * does
     * not exclude other keys that may have the same values.
     *
     * @throws IOException
     */
    @Test
    public void testKeyTourismValueNotInformationAnotherWay() throws IOException {
        Client client = getClient();
        Set<String> valueSet = ImmutableSet.of("information");
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withTagsNotValues(ImmutableMap.of("tourism", valueSet));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(6l));
    }

    /**
     * Find me all places named "berlin" that do not have the value "information" in their tags - no matter what key
     *
     * @throws IOException
     */
    @Test
    public void testValueNotInformation() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withoutValues("information");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(12l));
    }

    /**
     * Find me all places named "berlin" that do not have the key "tourism" in their tags
     *
     * @throws IOException
     */
    @Test
    public void testKeyNotTourism() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withoutKeys("tourism");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(8l));
    }

    @Test
    public void testValueInformationNotAmenity() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withValues("information").withoutKeys("amenity").withStrictMatch();
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(2l));
    }


    private PhotonDoc createDoc(int id, String key, String value) {
        ImmutableMap<String, String> nameMap = ImmutableMap.of("name", "berlin");
        Point location = FACTORY.createPoint(new Coordinate(10., 47.));
        return new PhotonDoc(id, "way", id, key, value, nameMap, null, null, null, 0, 0.5, null, location, 0, 0);
    }

    @After
    public void tearDownClass() {
        shutdownES();
    }
}

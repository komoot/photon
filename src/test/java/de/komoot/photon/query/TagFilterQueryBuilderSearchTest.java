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

    @Test
    public void testFilterWithTagTourismAttraction() throws IOException {
        Client client = getClient();
        Set<String> valueSet = ImmutableSet.of("attraction");
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withTags(ImmutableMap.of("tourism", valueSet));
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(2l));
    }

    @Test
    public void testKeyTourism() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withKeys("tourism");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(8l));
    }

    @Test
    public void testKeyTourismValueNotInformation() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withKeys("tourism").withoutValues("information");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(6l));
    }

    @Test
    public void testValueNotInformation() throws IOException {
        Client client = getClient();
        TagFilterQueryBuilder tagFilterQueryBuilder = PhotonQueryBuilder.builder("berlin").withoutValues("information");
        QueryBuilder queryBuilder = tagFilterQueryBuilder.buildQuery();
        SearchResponse searchResponse = client.prepareSearch("photon").setSearchType(SearchType.QUERY_AND_FETCH).setQuery(queryBuilder).execute().actionGet();
        assertThat(searchResponse.getHits().getTotalHits(), is(12l));
    }

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

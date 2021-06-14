package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.elasticsearch.Importer;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import de.komoot.photon.elasticsearch.PhotonIndex;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Before;
import org.junit.Test;


public class QueryFilterTagValueTest extends ESBaseTester {
    private static final String[] TAGS = new String[]{"tourism", "attraction", "tourism", "hotel", "tourism", "museum", "tourism", "information", "amenity",
            "parking", "amenity", "restaurant", "amenity", "information", "food", "information", "railway", "station"};

    @Before
    public void setUp() throws Exception {
        setUpES();
        Importer instance = makeImporter();
        double lon = 13.38886;
        double lat = 52.51704;
        for (int i = 0; i < TAGS.length; i++) {
            String key = TAGS[i];
            String value = TAGS[++i];
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

    private PhotonQueryBuilder baseQueryBerlin() {
        return PhotonQueryBuilder.builder("berlin", "en", Arrays.asList("en"), true);
    }

    private SearchResponse search(QueryBuilder queryBuilder) {
        return getClient().prepareSearch(PhotonIndex.NAME).setSearchType(SearchType.QUERY_THEN_FETCH).setQuery(queryBuilder).execute().actionGet();
    }

    /**
     * Find me all places named "berlin" that are tagged "tourism=attraction"
     */
    @Test
    public void testFilterWithTagTourismAttraction() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withTags(Collections.singletonMap("tourism", Collections.singleton("attraction")))
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(2l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that are tagged with a value of "attraction".
     */
    @Test
    public void testValueAttraction() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withValues("attraction")
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(2l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that are tagged with key "tourism".
     */
    @Test
    public void testKeyTourism() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withKeys("tourism")
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(8l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that are NOT tagged "tourism=attraction"
     */
    @Test
    public void testFilterWithoutTagTourismAttraction() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withoutTags(Collections.singletonMap("tourism", Collections.singleton("attraction")))
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(16l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that do not have the value "information" in their TAGS - no matter what key
     */
    @Test
    public void testValueNotInformation() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withoutValues("information")
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(12l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that do not have the key "tourism" in their TAGS
     */
    @Test
    public void testKeyNotTourism() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withoutKeys("tourism")
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(10l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not tagged with value "information".
     * <p/>
     * Note: This is a different method of achieving the same result as
     * {@link QueryFilterTagValueTest#testKeyTourismButValueNotInformation()}
     */
    @Test
    public void testKeyTourismAndValueNotInformation() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withKeys("tourism")
                .withoutValues("information")
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(6l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not tagged with value "information".
     * <p/>
     * Note: This is a different method of achieving the same result as
     * {@link QueryFilterTagValueTest#testKeyTourismAndValueNotInformation}.
     */
    @Test
    public void testKeyTourismButValueNotInformation() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withTagsNotValues(Collections.singletonMap("tourism", Collections.singleton("information")))
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(6l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that are tagged without the keys "tourism" and "amenity".
     */
    @Test
    public void testKeyNotTourismAndKeyNotAmenity() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withoutKeys("tourism", "amenity")
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(4l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not "amenity". This test works, but,
     * the use case does not make sense because by searching for key "tourism", this test already excludes places keyed
     * on "amenity"
     */
    @Test
    public void testKeyTourismAndKeyNotAmenity() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withKeys("tourism")
                .withoutKeys("amenity")
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(8l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that have value "information" but not key "amenity"
     */
    @Test
    public void testValueInformationButKeyNotAmenity() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withValues("information")
                .withoutKeys("amenity")
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(4l, searchResponse.getHits().getTotalHits());
    }

    /**
     * Find me all places named "berlin" that do not have the tag tourism=attraction
     */
    @Test
    public void testTagNotTourismAttraction() {
        QueryBuilder queryBuilder = baseQueryBerlin()
                .withoutTags(Collections.singletonMap("tourism", Collections.singleton("attraction")))
                .buildQuery();

        SearchResponse searchResponse = search(queryBuilder);

        assertEquals(16l, searchResponse.getHits().getTotalHits());
    }
}

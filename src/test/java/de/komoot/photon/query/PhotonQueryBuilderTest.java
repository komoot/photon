package de.komoot.photon.query;

import de.komoot.photon.ReflectionTestUtil;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by sachi_000 on 2/28/2015.
 */
public class PhotonQueryBuilderTest {

    @Test
    public void testNoMoreThan50WithLimit() throws Exception {
        TagFilterQueryBuilder queryBuilder = PhotonQueryBuilder.builder("");
        queryBuilder.withLimit(100);
        Integer actualLimit = ReflectionTestUtil.getFieldValue(queryBuilder, "limit");
        assertThat(actualLimit, equalTo(50));
    }

    @Test
    public void testNullOrZeroIs15WithLimit() throws Exception {
        TagFilterQueryBuilder queryBuilder = PhotonQueryBuilder.builder("");
        Integer[] testValues = {null, 0, -1};
        for (Integer testValue : testValues) {
            queryBuilder.withLimit(testValue);
            Integer actualLimit = ReflectionTestUtil.getFieldValue(queryBuilder, "limit");
            assertThat(actualLimit, equalTo(15));

        }
    }

    @Test
    public void testWithLimit() throws Exception {
        TagFilterQueryBuilder queryBuilder = PhotonQueryBuilder.builder("");
        queryBuilder.withLimit(5);
        Integer actualLimit = ReflectionTestUtil.getFieldValue(queryBuilder, "limit");
        assertThat(actualLimit, equalTo(5));
    }

}
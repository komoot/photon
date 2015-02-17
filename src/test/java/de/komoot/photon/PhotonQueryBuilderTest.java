package de.komoot.photon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class PhotonQueryBuilderTest {

    @Test
    public void testConstructor() throws IOException {

        Point locationForBias = new GeometryFactory(new PrecisionModel(), 4326).createPoint(new Coordinate(-87, 41));
        PhotonQueryBuilder photonQueryBuilder = PhotonQueryBuilder.builder("berlin", 15, locationForBias);
        FilteredQueryBuilder filteredQueryBuilder = ReflectionTestUtil.getFieldValue(photonQueryBuilder, PhotonQueryBuilder.class, "filteredQueryBuilder");

        BytesReference bytes = filteredQueryBuilder.toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null)).bytes();
        String createdJson = new String(bytes.toBytes(), "UTF-8");
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("json_queries/test_base_query.json");
        String expectedJsonString = IOUtils.toString(resourceAsStream);
        JsonNode actualJson = new ObjectMapper().readTree(createdJson);
        JsonNode expectedJson = new ObjectMapper().readTree(expectedJsonString);
        Assert.assertEquals(expectedJson, actualJson);
        System.out.println(actualJson.asToken());


    }

}
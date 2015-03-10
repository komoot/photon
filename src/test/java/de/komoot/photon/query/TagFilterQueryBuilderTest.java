
package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.utils.QueryToJson;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.script.ScriptScoreFunctionBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

public class TagFilterQueryBuilderTest {

    @Test
    public void testConstructor() throws IOException {

        TagFilterQueryBuilder photonQueryBuilder = PhotonQueryBuilder.builder("berlin");
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("json_queries/test_base_query.json");
        String expectedJsonString = IOUtils.toString(resourceAsStream);

        JsonNode actualJson = this.readJson(new QueryToJson().convert(photonQueryBuilder.buildQuery()));
        JsonNode expectedJson = this.readJson(expectedJsonString);
        Assert.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testWithLocation() throws IOException {
        TagFilterQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        ScriptScoreFunctionBuilder expectedLocationBiasSubQueryBuilder = ScoreFunctionBuilders.scriptFunction("location-biased-score", "groovy").param("lat", 80).param("lon", 10);
        FunctionScoreQueryBuilder mockFunctionScoreQueryBuilder = Mockito.mock(FunctionScoreQueryBuilder.class);
        ReflectionTestUtil.setFieldValue(berlinQuery, "queryBuilder", mockFunctionScoreQueryBuilder);
        ArgumentCaptor<ScriptScoreFunctionBuilder> locationBiasSubQueryArgumentCaptor = ArgumentCaptor.forClass(ScriptScoreFunctionBuilder.class);
        berlinQuery.withLocationBias(new GeometryFactory().createPoint(new Coordinate(10, 80)));
        Mockito.verify(mockFunctionScoreQueryBuilder, Mockito.times(1)).add(locationBiasSubQueryArgumentCaptor.capture());
        ScriptScoreFunctionBuilder actualLocationBiasSubQueryBuilder = locationBiasSubQueryArgumentCaptor.getValue();
        Assert.assertEquals(this.readJson(expectedLocationBiasSubQueryBuilder), this.readJson(actualLocationBiasSubQueryBuilder));
    }

    private JsonNode readJson(ToXContent queryBuilder) throws IOException {
        return this.readJson(queryBuilder.toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null)).bytes());
    }

    private JsonNode readJson(BytesReference jsonStringBytes) throws IOException {
        return this.readJson(new String(jsonStringBytes.toBytes(), "UTF-8"));
    }


    private JsonNode readJson(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        return objectMapper.readTree(jsonString);
    }


}
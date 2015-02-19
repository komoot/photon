package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import de.komoot.photon.ReflectionTestUtil;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.script.ScriptScoreFunctionBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PhotonQueryBuilderTest {

    @Test
    public void testConstructor() throws IOException {

        PhotonQueryBuilder photonQueryBuilder = PhotonQueryBuilder.builder("berlin");
        FilteredQueryBuilder filteredQueryBuilder = ReflectionTestUtil.getFieldValue(photonQueryBuilder, PhotonQueryBuilder.class, "filteredQueryBuilder");

        BytesReference actualQueryBytes = filteredQueryBuilder.toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null)).bytes();
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("json_queries/test_base_query.json");
        String expectedJsonString = IOUtils.toString(resourceAsStream);
        JsonNode actualJson = this.getJson(actualQueryBytes);
        JsonNode expectedJson = this.getJson(expectedJsonString);
        File outputFile = new File("C:\\Users\\sachi_000\\dev\\Projects\\photon\\src\\test\\resources\\json_queries\\output.json");
        new ObjectMapper().writeValue(outputFile, actualJson);

        Assert.assertEquals(expectedJson, actualJson);
    }

    private JsonNode getJson(BytesReference jsonStringBytes) throws IOException {
        return this.getJson(new String(jsonStringBytes.toBytes(),"UTF-8"));
    }
    private JsonNode getJson(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        return objectMapper.readTree(jsonString);
    }

    @Test
    public void testWithLimit(){
        PhotonQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        Assert.assertEquals(50,berlinQuery.getLimit().intValue());
        berlinQuery.withLimit(4000);
        Assert.assertEquals(4000,berlinQuery.getLimit().intValue());
    }
    
    @Test
    public void testWithLocation() throws IOException {
        PhotonQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        ScriptScoreFunctionBuilder expectedLocationBiasSubQueryBuilder = ScoreFunctionBuilders.scriptFunction("location-biased-score", "mvel").param("lat", 80).param("lon", 10);
        BytesReference bytesExpected = expectedLocationBiasSubQueryBuilder.toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null)).bytes();
        FunctionScoreQueryBuilder mockFunctionScoreQueryBuilder = Mockito.mock(FunctionScoreQueryBuilder.class);
        ReflectionTestUtil.setFieldValue(berlinQuery,"functionScoreQueryBuilder",mockFunctionScoreQueryBuilder);
        ArgumentCaptor<ScriptScoreFunctionBuilder> locationBiasSubQueryArgumentCaptor = ArgumentCaptor.forClass(ScriptScoreFunctionBuilder.class);
        berlinQuery.withLocation(new GeometryFactory().createPoint(new Coordinate(10, 80)));
        Mockito.verify(mockFunctionScoreQueryBuilder,Mockito.times(1)).add(locationBiasSubQueryArgumentCaptor.capture());
        ScriptScoreFunctionBuilder actualLocationBiasSubQueryBuilder = locationBiasSubQueryArgumentCaptor.getValue();
        BytesReference actualBytesExpected = actualLocationBiasSubQueryBuilder.toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null)).bytes();
        Assert.assertEquals(this.getJson(bytesExpected),this.getJson(actualBytesExpected));
    }
    
}
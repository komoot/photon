package de.komoot.photon.query;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("json_queries/test_base_query.json");
        String expectedJsonString = IOUtils.toString(resourceAsStream);
        JsonNode actualJson = this.readJson(photonQueryBuilder.buildQueryJson());
        JsonNode expectedJson = this.readJson(expectedJsonString);
        this.writeJson("C:\\Users\\sachi_000\\dev\\Projects\\photon\\src\\test\\resources\\json_queries\\output.json", actualJson);
        Assert.assertEquals(expectedJson, actualJson);
    }

    private void writeJson(String outputFile, JsonNode json) throws IOException {
        File file = new File(outputFile);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        objectMapper.writeValue(file, json);
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

    @Test
    public void testWithLimit() {
        PhotonQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        Assert.assertEquals(50, berlinQuery.getLimit().intValue());
        berlinQuery.withLimit(4000);
        Assert.assertEquals(4000, berlinQuery.getLimit().intValue());
    }

    @Test
    public void testWithLocation() throws IOException {
        PhotonQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        ScriptScoreFunctionBuilder expectedLocationBiasSubQueryBuilder = ScoreFunctionBuilders.scriptFunction("location-biased-score", "mvel").param("lat", 80).param("lon", 10);
        FunctionScoreQueryBuilder mockFunctionScoreQueryBuilder = Mockito.mock(FunctionScoreQueryBuilder.class);
        ReflectionTestUtil.setFieldValue(berlinQuery, "queryBuilder", mockFunctionScoreQueryBuilder);
        ArgumentCaptor<ScriptScoreFunctionBuilder> locationBiasSubQueryArgumentCaptor = ArgumentCaptor.forClass(ScriptScoreFunctionBuilder.class);
        berlinQuery.withLocation(new GeometryFactory().createPoint(new Coordinate(10, 80)));
        Mockito.verify(mockFunctionScoreQueryBuilder, Mockito.times(1)).add(locationBiasSubQueryArgumentCaptor.capture());
        ScriptScoreFunctionBuilder actualLocationBiasSubQueryBuilder = locationBiasSubQueryArgumentCaptor.getValue();
        Assert.assertEquals(this.readJson(expectedLocationBiasSubQueryBuilder), this.readJson(actualLocationBiasSubQueryBuilder));
    }

    @Test
    public void testWithTags() throws IOException {
        PhotonQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        InputStream expectedJsonString = this.getClass().getClassLoader().getResourceAsStream("withTags.json");
        berlinQuery.withTags(ImmutableMap.of("key1", "value1", "key2", "value2"));
        System.out.println(berlinQuery.buildQueryJson());
        this.writeJson("C:\\Users\\sachi_000\\dev\\Projects\\photon\\src\\test\\resources\\json_queries\\output_tags_filtered.json", this.readJson(berlinQuery.buildQueryJson()));
        JsonNode jsonNode = new ObjectMapper().readTree(expectedJsonString);
    }

    @Test
    public void testWithKeys() throws IOException {
        PhotonQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        InputStream expectedJsonString = this.getClass().getClassLoader().getResourceAsStream("withTags.json");
        berlinQuery.withKeys(ImmutableSet.of("value1", "value2"));
        System.out.println(berlinQuery.buildQueryJson());
        this.writeJson("C:\\Users\\sachi_000\\dev\\Projects\\photon\\src\\test\\resources\\json_queries\\output_keys_filtered.json", this.readJson(berlinQuery.buildQueryJson()));
        JsonNode jsonNode = new ObjectMapper().readTree(expectedJsonString);
    }

    @Test
    public void testWithValues() throws IOException {
        PhotonQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        InputStream expectedJsonString = this.getClass().getClassLoader().getResourceAsStream("withTags.json");
        berlinQuery.withValues(ImmutableSet.of("value1", "value2"));
        System.out.println(berlinQuery.buildQueryJson());
        this.writeJson("C:\\Users\\sachi_000\\dev\\Projects\\photon\\src\\test\\resources\\json_queries\\output_values_filtered.json", this.readJson(berlinQuery.buildQueryJson()));
        JsonNode jsonNode = new ObjectMapper().readTree(expectedJsonString);
    }

    @Test
    public void testWithTagsAndValues() throws IOException {
        PhotonQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        InputStream expectedJsonString = this.getClass().getClassLoader().getResourceAsStream("withTags.json");
        berlinQuery.withValues(ImmutableSet.of("value1", "value2"));
        berlinQuery.withTags(ImmutableMap.of("key1", "value1", "key2", "value2"));
        System.out.println(berlinQuery.buildQueryJson());
        this.writeJson("C:\\Users\\sachi_000\\dev\\Projects\\photon\\src\\test\\resources\\json_queries\\output_tags_and_values_filtered.json", this.readJson(berlinQuery
                                                                                                                                                                      .buildQueryJson()));
        JsonNode jsonNode = new ObjectMapper().readTree(expectedJsonString);
    }
    @Test
    public void testWithTagsKeysAndValues() throws IOException {
        PhotonQueryBuilder berlinQuery = PhotonQueryBuilder.builder("berlin");
        InputStream expectedJsonString = this.getClass().getClassLoader().getResourceAsStream("withTags.json");
        berlinQuery.withKeys(ImmutableSet.of("Key1", "Key2"));
        berlinQuery.withValues(ImmutableSet.of("value1", "value2"));
        berlinQuery.withTags(ImmutableMap.of("key1", "value1", "key2", "value2"));
        String queryJson = berlinQuery.buildQueryJson();
        System.out.println(queryJson);
        this.writeJson("C:\\Users\\sachi_000\\dev\\Projects\\photon\\src\\test\\resources\\json_queries\\output_tags_keys_and_values_filtered.json", this.readJson(queryJson));
        JsonNode jsonNode = new ObjectMapper().readTree(expectedJsonString);
    }
}
package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.searcher.LookupHandler;
import de.komoot.photon.searcher.ReverseHandler;
import de.komoot.photon.searcher.SearchHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ElasticsearchServer {
    private final List<Header> headers = new ArrayList<>(){};
    private final RestClientBuilder restClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    public final String serverUrl;
    private JsonpMapper jsonpMapper = new JacksonJsonpMapper();
    public ElasticsearchClient esClient;

    private final String DATABASE_VERSION = "0.3.7";
    private final String PROPERTY_DOCUMENT_ID = "DATABASE_PROPERTIES";
    private final String BASE_FIELD = "document_properties";
    private final String FIELD_VERSION = "database_version";
    private final String FIELD_LANGUAGES = "indexed_languages";

    public ElasticsearchServer(String serverUrl) {
        this.serverUrl = serverUrl;
        this.restClientBuilder = RestClient.builder(HttpHost.create(serverUrl));
    }

    public ElasticsearchServer headers(Header[] headers) {
        this.headers.addAll(List.of(headers));
        return this;
    }

    public ElasticsearchServer jsonpMapper(JsonpMapper jsonpMapper) {
        this.jsonpMapper = jsonpMapper;
        return this;
    }

    public ElasticsearchServer apiKey(String apiKey) {
        this.headers.add(new BasicHeader("Authorization", String.format("ApiKey %s", apiKey)));
        return this;
    }

    public ElasticsearchServer start() {
        if (!headers.isEmpty()) {
            restClientBuilder.setDefaultHeaders(headers.toArray(Header[]::new));
        }

        ElasticsearchTransport transport = new RestClientTransport(
                restClientBuilder.build(),
                jsonpMapper
        );

        esClient = new ElasticsearchClient(transport);

        log.info("Started elastic search client connected to " + serverUrl);

        return this;
    }

    public ElasticsearchServer createIndex(ObjectNode settings, String[] languages) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest.Builder()
                .index(PhotonIndex.NAME)
                .settings(fn -> fn.withJson(new StringReader(settings.toString())))
                .mappings(fn -> fn.withJson(new StringReader(IndexMapping.buildMinimalMappings().toString())))
                .build();

        esClient.indices().create(request);

        return this.saveDbProperties(new DatabaseProperties().setLanguages(languages));
    }

    public ElasticsearchServer updateMappings(ObjectNode mappings) throws IOException {
        PutMappingRequest request = new PutMappingRequest.Builder()
                .index(PhotonIndex.NAME)
                .withJson(new StringReader(mappings.toString()))
                .build();

        esClient.indices().putMapping(request);
        return this;
    }

    public ElasticsearchServer updateSettings(ObjectNode settings) throws IOException {
        loadDbProperties();

        esClient.indices().close(fn -> fn.index(PhotonIndex.NAME));

        PutIndicesSettingsRequest request = new PutIndicesSettingsRequest.Builder()
                .index(PhotonIndex.NAME)
                .withJson(new StringReader(settings.toString()))
                .build();

        esClient.indices().putSettings(request);

        esClient.indices().open(fn -> fn.index(PhotonIndex.NAME));

        return this;
    }

    public ElasticsearchServer deleteIndex() throws IOException {
        try {
            esClient.indices().delete(fn -> fn.index(PhotonIndex.NAME));
        } catch (ElasticsearchException exc) {
            log.error(exc.endpointId());
            log.error(exc.error().type());
        }
        return this;
    }

    public ElasticsearchServer waitForReady() throws IOException {
        esClient.cluster().health(fn -> fn.waitForStatus(HealthStatus.Yellow));
        return this;
    }

    public ElasticsearchServer saveDbProperties(DatabaseProperties dbProperties) throws IOException {
        final String DATABASE_VERSION = "0.3.7";
        final String PROPERTY_DOCUMENT_ID = "DATABASE_PROPERTIES";
        final String BASE_FIELD = "document_properties";
        final String FIELD_VERSION = "database_version";
        final String FIELD_LANGUAGES = "indexed_languages";

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set(
                BASE_FIELD,
                objectMapper.createObjectNode()
                        .put(FIELD_VERSION, DATABASE_VERSION)
                        .put(FIELD_LANGUAGES, String.join(",", dbProperties.getLanguages()))
        );
        esClient.index(fn -> fn.index(PhotonIndex.NAME).id(PROPERTY_DOCUMENT_ID).document(properties));
        return this;
    }

    /**
     * Load the global properties from the database.
     *
     * The function first loads the database version and throws an exception if it does not correspond
     * to the version as defined in DATABASE_VERSION.
     *
     * Currently, does nothing when the property entry is missing. Later versions with a higher
     * database version will then fail.
     */
    public DatabaseProperties loadDbProperties() throws IOException {
        GetResponse<ObjectNode> response = esClient.get(fn -> fn.index(PhotonIndex.NAME).id(PROPERTY_DOCUMENT_ID), ObjectNode.class);

        // We are currently at the database version where versioning was introduced.
        if (!response.found()) {
            return null;
        }

        ObjectNode source = response.source();

        if (source == null || !source.hasNonNull(BASE_FIELD)) {
            throw new RuntimeException("Found database properties but no '" + BASE_FIELD + "' field. Database corrupt?");
        }

        ObjectNode properties = (ObjectNode) source.get(BASE_FIELD);

        String version = properties.hasNonNull(FIELD_VERSION) ? properties.get(FIELD_VERSION).asText() : "";

        if (!DATABASE_VERSION.equals(version)) {
            log.error("Database has incompatible version '" + version + "'. Expected: " + DATABASE_VERSION);
            throw new RuntimeException("Incompatible database.");
        }

        String langString = String.valueOf(properties.get(FIELD_LANGUAGES));

        return new DatabaseProperties().setLanguages(langString == null ? null : langString.split(","));
    }


    public ElasticsearchServer recreateIndex(ObjectNode settings, ObjectNode mappings, String[] languages) throws IOException {
        return deleteIndex().createIndex(settings, languages).updateMappings(mappings);
    }

    public de.komoot.photon.Importer createImporter(String[] languages, String[] extraTags, boolean allExtraTags) {
        return new de.komoot.photon.elasticsearch.Importer(esClient, languages, extraTags, allExtraTags);
    }

    public de.komoot.photon.Updater createUpdater(String[] languages, String[] extraTags, boolean allExtraTags) {
        return new de.komoot.photon.elasticsearch.Updater(esClient, languages, extraTags, allExtraTags);
    }

    public SearchHandler createSearchHandler(String[] languages) {
        return new ElasticsearchSearchHandler(esClient, languages);
    }

    public ReverseHandler createReverseHandler() {
        return new ElasticsearchReverseHandler(esClient);
    }

    public LookupHandler createLookupHandler() {
        return new ElasticsearchLookupHandler(esClient);
    }
}

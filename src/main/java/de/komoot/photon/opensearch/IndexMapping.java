package de.komoot.photon.opensearch;

import org.jspecify.annotations.NullMarked;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch._types.mapping.IndexOptions;
import org.opensearch.client.opensearch.indices.PutMappingRequest;

import java.io.IOException;

@NullMarked
public class IndexMapping {
    private static final String[] ADDRESS_FIELDS = new String[]{"name", "street", "city", "district", "county", "state", "country"};

    private PutMappingRequest.Builder mappings;

    public IndexMapping() {
        setupBaseMappings();
    }

    public void putMapping(OpenSearchClient client, String indexName) throws IOException {
        client.indices().putMapping(mappings.index(indexName).build());
    }

    private void setupBaseMappings() {
        mappings = new PutMappingRequest.Builder();

        mappings.dynamic(DynamicMapping.False)
                .source(s -> s.excludes("collector", "categories"));

        // Only list fields here that need an index in some form. All other fields will
        // be passive fields saved in and retrivable by _source.

        for (var field : new String[]{"osm_key", "osm_value", "type"}) {
            mappings.properties(field, b -> b.keyword(p -> p
                    .index(true)
                    .docValues(false)
            ));
        }

        mappings.properties("categories", b -> b.text(p -> p
                .index(true)
                .indexOptions(IndexOptions.Docs)
                .norms(false)
                .analyzer("index_categories")
                .searchAnalyzer("keyword")
        ));

        mappings.properties("coordinate", b -> b.geoPoint(p -> p));
        mappings.properties("countrycode", b -> b.keyword(p -> p
                .index(true)
                .docValues(false)
        ));
        mappings.properties("importance", b -> b.float_(p -> p
                .index(false)
        ));

        mappings.properties("housenumber", b -> b.text(p -> p
                .index(true)
                .indexOptions(IndexOptions.Docs)
                .analyzer("index_housenumber")
                .searchAnalyzer("standard")
                .fields("full", f -> f.text(full -> full
                        .index(true)
                        .norms(false)
                        .indexOptions(IndexOptions.Docs)
                        .analyzer("lowercase_keyword")
                ))
        ));

        mappings.properties("postcode", b -> b.text(p -> p
                .index(true)
                .norms(false)
                .analyzer("index_raw")
        ));

        // General collectors.
        mappings.properties("collector.all", b -> b.text(p -> p
                .index(true)
                .norms(false)
                .indexOptions(IndexOptions.Freqs)
                .analyzer("index_fullword")
                .searchAnalyzer("search")
                .fields("ngram", ngramField -> ngramField.text(p1 -> p1
                        .index(true)
                        .norms(false)
                        .indexOptions(IndexOptions.Freqs)
                        .analyzer("index_ngram")
                        .searchAnalyzer("search")
                ))
        ));

        mappings.properties("collector.name", b -> b.text(p -> p
                .index(true)
                .norms(false)
                .indexOptions(IndexOptions.Freqs)
                .analyzer("index_name_ngram")
                .searchAnalyzer("search")
                .fields("prefix", prefixField -> prefixField.text(p1 -> p1
                        .index(true)
                        .norms(false)
                        .indexOptions(IndexOptions.Freqs)
                        .analyzer("index_name_prefix")
                        .searchAnalyzer("search_prefix")
                ))
        ));

        mappings.properties("collector.parent", b -> b.text(p -> p
                .index(true)
                .norms(false)
                .indexOptions(IndexOptions.Docs)
                .analyzer("index_name_ngram")
                .searchAnalyzer("search")
        ));

        for (var field : ADDRESS_FIELDS) {
            mappings.properties("collector.field." + field, b -> b.text(p -> p
                    .index(true)
                    .norms(false)
                    .analyzer("index_raw")
                    .searchAnalyzer("search")
                    .fields("full", prefixField -> prefixField.text(p1 -> p1
                            .index(true)
                            .norms(false)
                            .indexOptions(IndexOptions.Docs)
                            .analyzer("index_name_full")
                            .searchAnalyzer("search_prefix")
                    ))
            ));
        }
    }
}

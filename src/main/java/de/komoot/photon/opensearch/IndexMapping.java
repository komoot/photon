package de.komoot.photon.opensearch;

import org.jspecify.annotations.NullMarked;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch._types.mapping.IndexOptions;
import org.opensearch.client.opensearch.indices.PutMappingRequest;

import java.io.IOException;
import java.util.List;

@NullMarked
public class IndexMapping {
    private static final String[] ADDRESS_FIELDS = new String[]{
            DocFields.NAME, DocFields.STREET, DocFields.CITY,
            DocFields.DISTRICT, DocFields.COUNTY, DocFields.STATE, DocFields.COUNTRY};

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
                .source(s -> s.excludes(DocFields.COLLECTOR, DocFields.CATEGORIES));

        // Only list fields here that need an index in some form. All other fields will
        // be passive fields saved in and retrievable by _source.

        for (var field : List.of(DocFields.OSM_KEY, DocFields.OSM_VALUE, DocFields.OBJECT_TYPE)) {
            mappings.properties(field, b -> b.keyword(p -> p
                    .index(true)
                    .docValues(false)
            ));
        }

        mappings.properties(DocFields.CATEGORIES, b -> b.text(p -> p
                .index(true)
                .indexOptions(IndexOptions.Docs)
                .norms(false)
                .analyzer("index_categories")
                .searchAnalyzer("keyword")
        ));

        mappings.properties(DocFields.COORDINATE, b -> b.geoPoint(p -> p));
        mappings.properties(DocFields.COUNTRYCODE, b -> b.keyword(p -> p
                .index(true)
                .docValues(false)
        ));
        mappings.properties(DocFields.IMPORTANCE, b -> b.float_(p -> p
                .index(false)
        ));

        mappings.properties(DocFields.HOUSENUMBER, b -> b.text(p -> p
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

        mappings.properties(DocFields.POSTCODE, b -> b.text(p -> p
                .index(true)
                .norms(false)
                .analyzer("index_raw")
        ));

        // General collectors.
        mappings.properties(DocFields.COLLECTOR + ".all", b -> b.text(p -> p
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

        mappings.properties(DocFields.COLLECTOR + ".name", b -> b.text(p -> p
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

        mappings.properties(DocFields.COLLECTOR + ".parent", b -> b.text(p -> p
                .index(true)
                .norms(false)
                .indexOptions(IndexOptions.Docs)
                .analyzer("index_name_ngram")
                .searchAnalyzer("search")
        ));

        for (var field : ADDRESS_FIELDS) {
            mappings.properties(DocFields.COLLECTOR + ".field." + field, b -> b.text(p -> p
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

package de.komoot.photon.opensearch;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch.indices.PutMappingRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IndexMapping {
    private static final String[] ADDRESS_FIELDS = new String[]{"street", "city", "locality", "district", "county", "state", "country", "context"};

    private PutMappingRequest.Builder mappings;

    public IndexMapping() {
        setupBaseMappings();
    }

    public void putMapping(OpenSearchClient client, String indexName) throws IOException {
        client.indices().putMapping(mappings.index(indexName).build());
    }

    public IndexMapping addLanguages(String[] languages) {
        List<String> name_collectors = new ArrayList<>();
        for (var lang: languages) {
            mappings.properties("collector." + lang,
                    b -> b.text(p -> p.index(true)
                                      .analyzer("index_raw"))
            );

            for (var field: ADDRESS_FIELDS) {
                mappings.properties(String.format("%s.%s", field, lang),
                        b -> b.text(p -> p
                                .index(false)
                                .copyTo("collector.base", "collector." + lang)));
            }

            mappings.properties("name." + lang,
                    b -> b.text(p -> p.index(false)
                            .fields("ngrams", f -> f.text(pi -> pi.index(true).analyzer("index_ngram")))
                            .fields("raw", f2 -> f2.text(pi2 -> pi2.index(true).analyzer("index_raw")))
                            .copyTo("collector." + lang, "collector.base")));

            //add language-specific collector to default for name
            name_collectors.add("name." + lang);
        }

        name_collectors.add("collector.default");
        name_collectors.add("collector.base");
        mappings.properties("name.default", b -> b.text(p -> p.index(false).copyTo(name_collectors)));

        return this;
    }

    private void setupBaseMappings() {
        mappings = new PutMappingRequest.Builder();

        mappings.dynamic(DynamicMapping.False)
                .source(s -> s.excludes("context.*"));

        mappings.properties("osm_type", b -> b.text(p -> p.index(false)));
        mappings.properties("osm_id", b -> b.unsignedLong(l -> l.index(false)));

        for (var field : new String[]{"osm_key", "osm_value", "type"}) {
            mappings.properties(field, b -> b.keyword(p -> p.index(true)));
        }

        mappings.properties("coordinate", b -> b.geoPoint(p -> p));
        mappings.properties("countrycode", b -> b.text(p -> p.index(true)));
        mappings.properties("importance", b -> b.float_(p -> p.index(false)));

        mappings.properties("housenumber", b -> b.text(p -> p.index(true)
                .analyzer("index_housenumber").searchAnalyzer("standard")
                .copyTo("collector.default", "collector.base")
        ));

        mappings.properties("classification", b -> b.text(p -> p.index(true)
                .analyzer("keyword")
                .searchAnalyzer("search_classification")
                .copyTo("collector.default", "collector.base")));

        // The catch-all collector used to find overall matches.
        mappings.properties("collector.base", b -> b.text(p -> p
                .index(true)
                .analyzer("index_ngram")));

        // Collector for all address parts in the default language.
        mappings.properties("collector.default", b -> b.text(p -> p
                .index(true)
                .analyzer("index_raw")));

        for (var field : ADDRESS_FIELDS) {
            mappings.properties(field + ".default", b -> b.text(p -> p
                    .index(false)
                    .copyTo("collector.default", "collector.base")));
        }
        mappings.properties("postcode", b -> b.text(p -> p
                .index(false)
                .copyTo("collector.default", "collector.base")));

        mappings.properties("name.default", b -> b.text(p -> p
                .index(false)
                .copyTo("collector.default", "collector.base")));

        // Collector for all name parts.
        mappings.properties("name.other", b -> b.text(pi -> pi.index(true).analyzer("index_raw")));

        for (var suffix : new String[]{"alt", "int", "loc", "old", "reg", "housename"}) {
            mappings.properties("name." + suffix, b -> b.text(p -> p.index(false)
                    .copyTo("collector.default", "name.other", "collector.base")));
        }
    }
}

package de.komoot.photon.opensearch;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryFilter {
    private final List<String> categories;

    public CategoryFilter(String categories) {
        this.categories = Arrays.stream(categories.split(","))
                .map(s -> "#" + s)
                .collect(Collectors.toList());
    }

    public Query buildIncludeQuery() {
        return Query.of(fn -> fn.terms(t -> t
                .field("collector.all")
                .terms(tm -> tm.value(categories.stream()
                        .map(FieldValue::of)
                        .collect(Collectors.toList())))
        ));
    }

    public Query buildExcludeQuery() {
        return Query.of(fn -> fn.bool(outer -> outer
                .mustNot(categories.stream()
                        .map(s -> Query.of(q -> q.bool(inner -> inner
                                .must(m -> m.term(t -> t
                                        .field("collector.all")
                                        .value(FieldValue.of(s))
                                ))
                        )))
                        .collect(Collectors.toList())))
        );
    }
}

package de.komoot.photon.opensearch;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryFilter {
    private final List<FieldValue> categories;

    public CategoryFilter(String filterTerm) {
        this.categories = Arrays.stream(filterTerm.split(","))
                .map(s -> FieldValue.of(s))
                .collect(Collectors.toList());
    }

    public Query buildIncludeQuery() {
        return Query.of(fn -> fn.terms(t -> t
                .field("categories")
                .terms(tm -> tm.value(categories)
                ))
        );
    }

    public Query buildExcludeQuery() {
        return Query.of(fn -> fn.bool(outer -> outer
                .should(categories.stream()
                        .map(s -> Query.of(q -> q.bool(inner -> inner
                                .mustNot(m -> m.term(t -> t
                                        .field("categories")
                                        .value(s)
                                ))
                        )))
                        .collect(Collectors.toList())))
        );
    }
}

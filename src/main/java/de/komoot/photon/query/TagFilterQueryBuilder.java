package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.Map;
import java.util.Set;

/**
 * Created by sachi_000 on 2/20/2015.
 */
public interface TagFilterQueryBuilder {
    TagFilterQueryBuilder withLimit(Integer limit);

    TagFilterQueryBuilder withLocationBias(Point point);

    TagFilterQueryBuilder withTags(Map<String, Set<String>> tags);

    TagFilterQueryBuilder withKeys(Set<String> keys);

    TagFilterQueryBuilder withValues(Set<String> values);

    TagFilterQueryBuilder withoutTags(Map<String, Set<String>> tagsToExclude);

    TagFilterQueryBuilder withoutKeys(Set<String> keysToExclude);

    TagFilterQueryBuilder withoutValues(Set<String> valuesToExclude);
    
    TagFilterQueryBuilder withKeys(String... keys);

    TagFilterQueryBuilder withValues(String... values);

    TagFilterQueryBuilder withoutKeys(String... keysToExclude);

    TagFilterQueryBuilder withoutValues(String... valuesToExclude);
    TagFilterQueryBuilder withStrictMatch();
    TagFilterQueryBuilder withLenientMatch();


    QueryBuilder buildQuery();

    Integer getLimit();
}

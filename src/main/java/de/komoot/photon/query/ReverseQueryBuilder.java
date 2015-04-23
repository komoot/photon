package de.komoot.photon.query;

import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Point;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.common.unit.DistanceUnit;

/**
 *
 * @author svantulden
 */
public class ReverseQueryBuilder implements TagFilterQueryBuilder {
    private Integer limit = 1;
    private State state;
    private Point location;

    private ReverseQueryBuilder(Point location) {
        this.state = State.PLAIN;
        this.location = location;
    }
    
    public static TagFilterQueryBuilder builder(Point location) {
        return new ReverseQueryBuilder(location);
    }

    @Override
    public TagFilterQueryBuilder withLimit(Integer limit) {        
        return this;
    }

    @Override
    public TagFilterQueryBuilder withLocationBias(Point point) {        
        return this;
    }

    @Override
    public TagFilterQueryBuilder withTags(Map<String, Set<String>> tags) {        
        return this;
    }

    @Override
    public TagFilterQueryBuilder withKeys(Set<String> keys) {
        return this;
    }

    @Override
    public TagFilterQueryBuilder withValues(Set<String> values) {
        return this;
    }

    @Override
    public TagFilterQueryBuilder withTagsNotValues(Map<String, Set<String>> tags) {
        return this;
    }

    @Override
    public TagFilterQueryBuilder withoutTags(Map<String, Set<String>> tagsToExclude) {
        return this;
    }

    @Override
    public TagFilterQueryBuilder withoutKeys(Set<String> keysToExclude) {
        return this;
    }

    @Override
    public TagFilterQueryBuilder withoutValues(Set<String> valuesToExclude) {
        return this;
    }

    @Override
    public TagFilterQueryBuilder withKeys(String... keys) {
        return this.withKeys(ImmutableSet.<String>builder().add(keys).build());
    }

    @Override
    public TagFilterQueryBuilder withValues(String... values) {
        return this.withValues(ImmutableSet.<String>builder().add(values).build());
    }

    @Override
    public TagFilterQueryBuilder withoutKeys(String... keysToExclude) {
        return this.withoutKeys(ImmutableSet.<String>builder().add(keysToExclude).build());
    }

    @Override
    public TagFilterQueryBuilder withoutValues(String... valuesToExclude) {
        return this.withoutValues(ImmutableSet.<String>builder().add(valuesToExclude).build());
    }

    @Override
    public TagFilterQueryBuilder withStrictMatch() {
        return this;
    }

    @Override
    public TagFilterQueryBuilder withLenientMatch() {
        return this;
    }
    
    @Override
    public QueryBuilder buildQuery() {
        FilterBuilder fb = FilterBuilders.geoDistanceFilter("coordinate")    
                            .point(location.getY(), location.getX())                                         
                            .distance(5, DistanceUnit.KILOMETERS)                 
                            .optimizeBbox("memory");
        return QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), fb);
    }

    @Override
    public Integer getLimit() {
        return limit;
    }

    private Boolean checkTags(Set<String> keys) {
        return !(keys == null || keys.isEmpty());
    }

    private Boolean checkTags(Map<String, Set<String>> tags) {
        return !(tags == null || tags.isEmpty());
    }

    private enum State {
        PLAIN, FILTERED, QUERY_ALREADY_BUILT, FINISHED,
    }
}

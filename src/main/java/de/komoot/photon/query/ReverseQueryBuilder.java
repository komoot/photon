package de.komoot.photon.query;



import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Point;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.common.unit.DistanceUnit;



/**
 *
 * @author svantulden
 */
public class ReverseQueryBuilder implements TagFilterQueryBuilder
{
    private Integer limit;

    private Double radius;

    private State state;

    private Point location;



    private ReverseQueryBuilder(Point location, Double radius)
    {
        this.state = State.PLAIN;
        this.location = location;
        this.radius = radius;
    }



    public static TagFilterQueryBuilder builder(Point location, Double radius)
    {
        return new ReverseQueryBuilder(location, radius);
    }



    @Override
    public TagFilterQueryBuilder withLimit(Integer limit)
    {
        this.limit = limit == null || limit < 0 ? 0 : limit;
        this.limit = this.limit > 50 ? 50 : this.limit;
        return this;
    }



    @Override
    public TagFilterQueryBuilder withLocationBias(Point point)
    {
        return this;
    }



    @Override
    public TagFilterQueryBuilder withTags(Map<String, Set<String>> tags)
    {
        return this;
    }



    @Override
    public TagFilterQueryBuilder withKeys(Set<String> keys)
    {
        return this;
    }



    @Override
    public TagFilterQueryBuilder withValues(Set<String> values)
    {
        return this;
    }



    @Override
    public TagFilterQueryBuilder withTagsNotValues(Map<String, Set<String>> tags)
    {
        return this;
    }



    @Override
    public TagFilterQueryBuilder withoutTags(Map<String, Set<String>> tagsToExclude)
    {
        return this;
    }



    @Override
    public TagFilterQueryBuilder withoutKeys(Set<String> keysToExclude)
    {
        return this;
    }



    @Override
    public TagFilterQueryBuilder withoutValues(Set<String> valuesToExclude)
    {
        return this;
    }



    @Override
    public TagFilterQueryBuilder withKeys(String... keys)
    {
        return this.withKeys(ImmutableSet.<String>builder().add(keys).build());
    }



    @Override
    public TagFilterQueryBuilder withValues(String... values)
    {
        return this.withValues(ImmutableSet.<String>builder().add(values).build());
    }



    @Override
    public TagFilterQueryBuilder withoutKeys(String... keysToExclude)
    {
        return this.withoutKeys(ImmutableSet.<String>builder().add(keysToExclude).build());
    }



    @Override
    public TagFilterQueryBuilder withoutValues(String... valuesToExclude)
    {
        return this.withoutValues(ImmutableSet.<String>builder().add(valuesToExclude).build());
    }



    @Override
    public TagFilterQueryBuilder withStrictMatch()
    {
        return this;
    }



    @Override
    public TagFilterQueryBuilder withLenientMatch()
    {
        return this;
    }



    @Override
    public QueryBuilder buildQuery()
    {
        QueryBuilder fb = QueryBuilders.geoDistanceQuery("coordinate").point(location.getY(), location.getX()).distance(radius, DistanceUnit.KILOMETERS);

        return QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery()).filter(fb);
    }



    @Override
    public Integer getLimit()
    {
        return limit;
    }



    private Boolean checkTags(Set<String> keys)
    {
        return !(keys == null || keys.isEmpty());
    }



    private Boolean checkTags(Map<String, Set<String>> tags)
    {
        return !(tags == null || tags.isEmpty());
    }




    private enum State {
        PLAIN, FILTERED, QUERY_ALREADY_BUILT, FINISHED,
    }
}

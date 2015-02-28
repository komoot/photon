package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.Map;
import java.util.Set;

/**
 * A query builder wrapper on top of {@link QueryBuilder elastic search api} specifically to handle <a href="http://taginfo.openstreetmap.org/projects/nominatim">OSM tags</a> as
 * {@link org.elasticsearch.index.query.FilterBuilder elastic search filter} criteria in the context of photon.
 * <p/>
 * Created by Sachin Dole on 2/20/2015.
 */
public interface TagFilterQueryBuilder {
    /**
     * Limit for number of results to return. Default is delegated to elastic search and max limit is 50.
     *
     * @param limit number of search results to return.
     *
     * @return
     */
    TagFilterQueryBuilder withLimit(Integer limit);

    /**
     * Location bias for query. By setting this, places found will be sorted by proximity to this point.
     *
     * @param point Geographical {@link Point}
     */
    TagFilterQueryBuilder withLocationBias(Point point);

    /**
     * Search results will be filtered to contain places having tags as provided in the argument. For example, if the argument contains
     * <pre>
     *     key="toursim", value = Set of "attractions"
     * </pre>
     * then, only those places that have the osm tag key of "tourism" and value of "attraction" will be included in search results.
     *
     * @param tags {@link Map} of osm tag keys to a set of values for that key.
     */
    TagFilterQueryBuilder withTags(Map<String, Set<String>> tags);

    /**
     * Similar to {@link TagFilterQueryBuilder#withTags(Map)} except that this allows clients to filter for places containing only the keys in the osm tag, without regard to what
     * the value may be.
     *
     * @param keys {@link Set} of osm tag keys
     */
    TagFilterQueryBuilder withKeys(Set<String> keys);

    /**
     * Similar to {@link TagFilterQueryBuilder#withTags(Map)} except that this allows clients to filter for places containing only the values in the osm tag, without regard to what
     * the key may be.
     *
     * @param values {@link Set} of osm tag values
     */
    TagFilterQueryBuilder withValues(Set<String> values);

    /**
     * Filter search results to include only those places that have a specified osm tag key, but, exclude all places that have the specified values within that tag. For example,
     * consider 3 places
     * <pre>
     *     berlin, key=tourism, value=attraction
     *     copenhagen, key=tourism, value=museum
     *     chicago, key=tourism, value=information
     * </pre>
     * then, argument
     * <pre>
     *     Map with key "tourism" and values "attraction", "museum"
     * </pre>
     * will result in only one result i.e.
     * <pre>
     *     chicago, key=tourism, value=information
     * </pre>
     */
    TagFilterQueryBuilder withTagsNotValues(Map<String, Set<String>> tags);

    /**
     * This is exactly same as {@link TagFilterQueryBuilder#withTagsNotValues(Map)} except the names of these two methods hint at how these elastic search filters may be
     * implemented. This alluded choice of implementing the filter leads to different ways in which elastic search may cache results.
     * <p/>
     * Exclude all places that have the specified osm tags.
     */
    TagFilterQueryBuilder withoutTags(Map<String, Set<String>> tagsToExclude);

    /**
     * Exclude places that have specified osm tag keys
     */
    TagFilterQueryBuilder withoutKeys(Set<String> keysToExclude);

    /**
     * Exclude places that have specified osm tag values
     */
    TagFilterQueryBuilder withoutValues(Set<String> valuesToExclude);

    /**
     * Include only those places that have specified osm tag keys
     */
    TagFilterQueryBuilder withKeys(String... keys);

    /**
     * Include only those places that have specified osm tag values
     *
     */

    /**
     * Include only those places that have specified osm tag values
     */
    TagFilterQueryBuilder withValues(String... values);

    /**
     * Exclude places that have specified osm tag keys
     */
    TagFilterQueryBuilder withoutKeys(String... keysToExclude);

    /**
     * Exclude places that have specified osm tag values
     */
    TagFilterQueryBuilder withoutValues(String... valuesToExclude);

    /**
     * {@link org.elasticsearch.index.query.MatchQueryBuilder match} exactly
     *
     * @TODO improve this documentation.
     */
    TagFilterQueryBuilder withStrictMatch();

    /**
     * allow lenient {@link org.elasticsearch.index.query.MatchQueryBuilder match}
     *
     * @TODO improve this documentation.
     */
    TagFilterQueryBuilder withLenientMatch();


    /**
     * turn all collected tags, values, inclusions, exclusions into a {@link QueryBuilder} that can be executed on elastic search
     */
    QueryBuilder buildQuery();

    Integer getLimit();
}

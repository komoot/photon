package de.komoot.photon.query;

import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

/**
 * There are four {@link de.komoot.photon.query.PhotonQueryBuilder.State states} that this query builder goes through before a query can be executed on elastic search. Of these,
 * three are of importance. <ul> <li>{@link de.komoot.photon.query.PhotonQueryBuilder.State#PLAIN PLAIN} The query builder is being used to build a query without any tag filters.
 * </li> <li>{@link de.komoot.photon.query.PhotonQueryBuilder.State#FILTERED FILTERED} The query builder is being used to build a query that has tag filters and can no longer be
 * used to build a PLAIN filter. </li> <li>{@link de.komoot.photon.query.PhotonQueryBuilder.State#FINISHED FINISHED} The query builder has been built and the query has been placed
 * inside a {@link QueryBuilder filtered query}. Further calls to any methods will have no effect on this query builder.</li>  </ul>
 * <p/>
 * Created by Sachin Dole on 2/12/2015.
 *
 * @see de.komoot.photon.query.TagFilterQueryBuilder
 */
public class PhotonQueryBuilder implements TagFilterQueryBuilder {
	private FunctionScoreQueryBuilder queryBuilder;
	private Integer limit = 50;
	private QueryBuilder queryBuilderForTopLevelFilter;
	private State state;
	private OrQueryBuilder orQueryBuilderForIncludeTagFiltering = null;
	private AndQueryBuilder andQueryBuilderForExcludeTagFiltering = null;
	private MatchQueryBuilder defaultMatchQueryBuilder;
	private MatchQueryBuilder languageMatchQueryBuilder;
	private QueryBuilder finalQueryBuilder;

	private PhotonQueryBuilder(String query, String language) {
		defaultMatchQueryBuilder = QueryBuilders.
				matchQuery("collector.default", query).
				fuzziness(Fuzziness.ONE).
				prefixLength(2).
				analyzer("search_ngram").
				minimumShouldMatch("100%");
		languageMatchQueryBuilder = QueryBuilders.
				matchQuery(String.format("collector.%s.ngrams", language), query).
				fuzziness(Fuzziness.ONE).
				prefixLength(2).
				analyzer("search_ngram").
				minimumShouldMatch("100%");

		queryBuilder = QueryBuilders.functionScoreQuery(
				QueryBuilders.boolQuery().must(
						QueryBuilders.boolQuery().should(
								defaultMatchQueryBuilder
						).should(
								languageMatchQueryBuilder
						).minimumShouldMatch("1")
				).should(
						QueryBuilders.matchQuery(String.format("name.%s.raw", language), query).boost(200).analyzer("search_raw")
				).should(
						QueryBuilders.matchQuery(String.format("collector.%s.raw", language), query).boost(100).analyzer("search_raw")
				),

				ScoreFunctionBuilders.scriptFunction(new Script("general-score", ScriptService.ScriptType.FILE, "groovy", null))
		).boostMode("multiply").scoreMode("multiply");
		queryBuilderForTopLevelFilter = QueryBuilders.orQuery(
				QueryBuilders.missingQuery("housenumber"),
				QueryBuilders.queryFilter(
						QueryBuilders.matchQuery("housenumber", query).analyzer("standard")
				),
				QueryBuilders.existsQuery(String.format("name.%s.raw", language))
		);
		state = State.PLAIN;
	}

	/**
	 * Create an instance of this builder which can then be embellished as needed.
	 *
	 * @param query    the value for photon query parameter "q"
	 * @param language
	 * @return An initialized {@link TagFilterQueryBuilder photon query builder}.
	 */
	public static TagFilterQueryBuilder builder(String query, String language) {
		return new PhotonQueryBuilder(query, language);
	}

	@Override
	public TagFilterQueryBuilder withLimit(Integer limit) {
		this.limit = limit == null || limit < 1 ? 15 : limit;
		this.limit = this.limit > 50 ? 50 : this.limit;
		return this;
	}

	@Override
	public TagFilterQueryBuilder withLocationBias(Point point) {
		if(point == null) return this;
		Map<String,Object> params =  newHashMap();
		params.put("lon",  point.getX());
		params.put("lat",  point.getY());
		queryBuilder.add(ScoreFunctionBuilders.scriptFunction(new Script("location-biased-score", ScriptService.ScriptType.FILE, "groovy", params)));
		return this;
	}

	@Override
	public TagFilterQueryBuilder withTags(Map<String, Set<String>> tags) {
		if(!checkTags(tags)) return this;
		ensureFiltered();
		List<AndQueryBuilder> termQueries = new ArrayList<AndQueryBuilder>(tags.size());
		for(String tagKey : tags.keySet()) {
			Set<String> valuesToInclude = tags.get(tagKey);
			TermQueryBuilder keyQuery = QueryBuilders.termQuery("osm_key", tagKey);
			TermsQueryBuilder valueQuery = QueryBuilders.termsQuery("osm_value", valuesToInclude.toArray(new String[valuesToInclude.size()]));
			AndQueryBuilder includeAndQuery = QueryBuilders.andQuery(keyQuery, valueQuery);
			termQueries.add(includeAndQuery);
		}
		this.appendIncludeTermQueries(termQueries);
		return this;
	}

	@Override
	public TagFilterQueryBuilder withKeys(Set<String> keys) {
		if(!checkTags(keys)) return this;
		ensureFiltered();
		List<TermsQueryBuilder> termQueries = new ArrayList<TermsQueryBuilder>(keys.size());
		termQueries.add(QueryBuilders.termsQuery("osm_key", keys.toArray()));
		this.appendIncludeTermQueries(termQueries);
		return this;
	}

	@Override
	public TagFilterQueryBuilder withValues(Set<String> values) {
		if(!checkTags(values)) return this;
		ensureFiltered();
		List<TermsQueryBuilder> termQueries = new ArrayList<TermsQueryBuilder>(values.size());
		termQueries.add(QueryBuilders.termsQuery("osm_value", values.toArray(new String[values.size()])));
		this.appendIncludeTermQueries(termQueries);
		return this;
	}

	@Override
	public TagFilterQueryBuilder withTagsNotValues(Map<String, Set<String>> tags) {
		if(!checkTags(tags)) return this;
		ensureFiltered();
		List<AndQueryBuilder> termQueries = new ArrayList<AndQueryBuilder>(tags.size());
		for(String tagKey : tags.keySet()) {
			Set<String> valuesToInclude = tags.get(tagKey);
			TermQueryBuilder keyQuery = QueryBuilders.termQuery("osm_key", tagKey);
			TermsQueryBuilder valueQuery = QueryBuilders.termsQuery("osm_value", valuesToInclude.toArray(new String[valuesToInclude.size()]));
			NotQueryBuilder negatedValueQuery = QueryBuilders.notQuery(valueQuery);
			AndQueryBuilder includeAndQuery = QueryBuilders.andQuery(keyQuery, negatedValueQuery);
			termQueries.add(includeAndQuery);
		}
		this.appendIncludeTermQueries(termQueries);
		return this;
	}

	@Override
	public TagFilterQueryBuilder withoutTags(Map<String, Set<String>> tagsToExclude) {
		if(!checkTags(tagsToExclude)) return this;
		ensureFiltered();
		List<NotQueryBuilder> termQueries = new ArrayList<NotQueryBuilder>(tagsToExclude.size());
		for(String tagKey : tagsToExclude.keySet()) {
			Set<String> valuesToExclude = tagsToExclude.get(tagKey);
			TermQueryBuilder keyQuery = QueryBuilders.termQuery("osm_key", tagKey);
			TermsQueryBuilder valueQuery = QueryBuilders.termsQuery("osm_value", valuesToExclude.toArray(new String[valuesToExclude.size()]));
			AndQueryBuilder andQueryForExclusions = QueryBuilders.andQuery(keyQuery, valueQuery);
			termQueries.add(QueryBuilders.notQuery(andQueryForExclusions));
		}
		this.appendExcludeTermQueries(termQueries);
		return this;
	}

	@Override
	public TagFilterQueryBuilder withoutKeys(Set<String> keysToExclude) {
		if(!checkTags(keysToExclude)) return this;
		ensureFiltered();
		List<NotQueryBuilder> termQueries = new ArrayList<NotQueryBuilder>(keysToExclude.size());
		termQueries.add(QueryBuilders.notQuery(QueryBuilders.termsQuery("osm_key", keysToExclude.toArray())));
		this.appendExcludeTermQueries(termQueries);
		return this;
	}

	@Override
	public TagFilterQueryBuilder withoutValues(Set<String> valuesToExclude) {
		if(!checkTags(valuesToExclude)) return this;
		ensureFiltered();
		List<NotQueryBuilder> termQueries = new ArrayList<NotQueryBuilder>(valuesToExclude.size());
		termQueries.add(QueryBuilders.notQuery(QueryBuilders.termsQuery("osm_value", valuesToExclude.toArray())));
		this.appendExcludeTermQueries(termQueries);
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
		defaultMatchQueryBuilder.minimumShouldMatch("100%");
		languageMatchQueryBuilder.minimumShouldMatch("100%");
		return this;
	}

	@Override
	public TagFilterQueryBuilder withLenientMatch() {
		defaultMatchQueryBuilder.minimumShouldMatch("-1");
		languageMatchQueryBuilder.minimumShouldMatch("-1");
		return this;
	}

	/**
	 * When this method is called, all filters are placed inside their {@link OrQueryBuilder OR} or {@link AndQueryBuilder AND} containers and the top level filter builder is
	 * built. Subsequent invocations of this method have no additional effect. Note that after this method is called, calling other methods on this class also have no effect.
	 *
	 * @see TagFilterQueryBuilder#buildQuery()
	 */
	@Override
	public QueryBuilder buildQuery() {
		if(state.equals(State.FINISHED)) {
			return finalQueryBuilder;
		}
		if(state.equals(State.FILTERED)) {
			if(orQueryBuilderForIncludeTagFiltering != null)
				((AndQueryBuilder) queryBuilderForTopLevelFilter).add(orQueryBuilderForIncludeTagFiltering);
			if(andQueryBuilderForExcludeTagFiltering != null)
				((AndQueryBuilder) queryBuilderForTopLevelFilter).add(andQueryBuilderForExcludeTagFiltering);
		}
		state = State.FINISHED;
		finalQueryBuilder = QueryBuilders.filteredQuery(queryBuilder, queryBuilderForTopLevelFilter);
		return finalQueryBuilder;
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

	private void appendIncludeTermQueries(List<? extends QueryBuilder> termQueries) {
		if(orQueryBuilderForIncludeTagFiltering == null) {
			orQueryBuilderForIncludeTagFiltering = QueryBuilders.orQuery(termQueries.toArray(new QueryBuilder[termQueries.size()]));
		} else {
			for(QueryBuilder eachTagFilter : termQueries) {
				orQueryBuilderForIncludeTagFiltering.add(eachTagFilter);
			}
		}
	}

	private void appendExcludeTermQueries(List<NotQueryBuilder> termQueries) {
		if(andQueryBuilderForExcludeTagFiltering == null) {
			andQueryBuilderForExcludeTagFiltering = QueryBuilders.andQuery(termQueries.toArray(new QueryBuilder[termQueries.size()]));
		} else {
			for(QueryBuilder eachTagFilter : termQueries) {
				andQueryBuilderForExcludeTagFiltering.add(eachTagFilter);
			}
		}
	}

	private void ensureFiltered() {
		if(state.equals(State.PLAIN)) {
			queryBuilderForTopLevelFilter = QueryBuilders.andQuery(queryBuilderForTopLevelFilter);
		} else if(queryBuilderForTopLevelFilter instanceof AndQueryBuilder) {
			//good! nothing to do because query builder is already filtered.
		} else {
			throw new RuntimeException("This code is not in valid state. It is expected that the filter builder field should either be AndQueryBuilder or OrQueryBuilder. Found" +
					" " + queryBuilderForTopLevelFilter.getClass() + " instead.");
		}
		state = State.FILTERED;
	}

	private enum State {
		PLAIN, FILTERED, QUERY_ALREADY_BUILT, FINISHED,
	}
}

package de.komoot.photon.searcher;

import de.komoot.photon.query.FilteredPhotonRequest;
import de.komoot.photon.query.PhotonQueryBuilder;
import de.komoot.photon.query.TagFilterQueryBuilder;

import java.util.Map;
import java.util.Set;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class FilteredPhotonRequestHandler extends AbstractPhotonRequestHandler<FilteredPhotonRequest> implements PhotonRequestHandler<FilteredPhotonRequest> {

    public FilteredPhotonRequestHandler(ElasticsearchSearcher elasticsearchSearcher) {
        super(elasticsearchSearcher);
    }
    
    @Override
    public TagFilterQueryBuilder buildQuery(FilteredPhotonRequest photonRequest) {
        String query = photonRequest.getQuery();
        Map<String, Set<String>> includeTags = photonRequest.tags();
        Set<String> includeKeys = photonRequest.keys();
        Set<String> includeValues = photonRequest.values();
        Map<String, Set<String>> excludeTags = photonRequest.notTags();
        Set<String> excludeKeys = photonRequest.notKeys();
        Set<String> excludeValues = photonRequest.notValues();
        Map<String, Set<String>> excludeTagValues = photonRequest.tagNotValues();
        return PhotonQueryBuilder.
                                         builder(query).
                                         withTags(includeTags).
                                         withKeys(includeKeys).
                                         withValues(includeValues).
                                         withoutTags(excludeTags).
                                         withoutKeys(excludeKeys).
                                         withoutValues(excludeValues).
                                         withTagsNotValues(excludeTagValues);
    }

}

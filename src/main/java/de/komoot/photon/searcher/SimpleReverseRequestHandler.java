package de.komoot.photon.searcher;

import de.komoot.photon.query.ReverseQueryBuilder;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.TagFilterQueryBuilder;
/**
 *
 * @author svantulden
 */
public class SimpleReverseRequestHandler extends AbstractReverseRequestHandler<ReverseRequest> implements ReverseRequestHandler<ReverseRequest> {
    public SimpleReverseRequestHandler(ElasticsearchReverseSearcher elasticsearchSearcher) {
        super(elasticsearchSearcher);
    }

    @Override
    public TagFilterQueryBuilder buildQuery(ReverseRequest photonRequest) {
        return ReverseQueryBuilder.builder(photonRequest.getLocation());
    }
}

package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.PhotonResult;
import org.jspecify.annotations.NullMarked;
import org.opensearch.client.opensearch.core.search.SearchResult;

import java.util.Comparator;
import java.util.stream.Stream;

@NullMarked
public class ResultScorer {
    private double maxScore = 10.0;

    public static Stream<OpenSearchResult> hitsToResultStream(SearchResult<OpenSearchResult> results) {
        Stream.Builder<OpenSearchResult> builder = Stream.builder();
        for (var hit: results.hits().hits()) {
            var result = hit.source();
            if (result != null) {
                result.setOpensearchScore(hit.score());
                builder.add(result);
            }
        }

        return builder.build();
    }

    static public Stream<PhotonResult> adjustByNormalizedOpenSearchScore(Stream<OpenSearchResult> results) {
        var scorer = new ResultScorer();
        return results.sorted(Comparator.comparingDouble(OpenSearchResult::getOpensearchScore).reversed())
                .map(scorer::normalizeScore);
    }

     private PhotonResult normalizeScore(OpenSearchResult result) {
        var osScore = result.getOpensearchScore();

        if (osScore >= maxScore) {
            maxScore = osScore;
            result.adjustScore(1.0);
        } else if (maxScore < 20) {
            result.adjustScore(osScore / maxScore);
        } else if (osScore > maxScore - 20) {
            result.adjustScore((osScore - maxScore + 20) / 20);
        }

        return result;
     }
}

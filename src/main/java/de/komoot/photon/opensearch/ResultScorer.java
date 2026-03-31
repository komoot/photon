package de.komoot.photon.opensearch;

import org.jspecify.annotations.NullMarked;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.SearchResult;

import java.util.function.Consumer;
import java.util.stream.Stream;

@NullMarked
public class ResultScorer {
    private final double importanceWeight;

    private double maxScore = 10.0;

    public ResultScorer(double importanceWeight) {
        this.importanceWeight = importanceWeight;
    }

    public static Stream<OpenSearchResult> hitsToResultStream(SearchResult<OpenSearchResult> results,
                                                          double importanceWeight) {
        var scorer = new ResultScorer(importanceWeight);
        return results.hits().hits().stream()
                .mapMulti(scorer::hitToResult)
                .toList().stream()  // go through all results once before normalizing
                .map(scorer::normalizeScore);
    }

     public void hitToResult(Hit<OpenSearchResult> hit, Consumer<OpenSearchResult> consumer) {
        var result = hit.source();
        if (result != null) {
            var score = hit.score();
            if (score != null) {
                if (importanceWeight > 0) {
                    score -= result.getImportance() * importanceWeight;
                }
                if (score > maxScore) {
                    maxScore = score;
                }
                result.setOpensearchScore(score);
            }

            consumer.accept(result);
        }
     }

     private OpenSearchResult normalizeScore(OpenSearchResult result) {
        var osScore = result.getOpensearchScore();

        if (maxScore < 20) {
            osScore /= maxScore;
        } else if (osScore <= maxScore - 20) {
            osScore = 0;
        } else {
            osScore = (osScore - maxScore + 20) / 20;
        }

        result.adjustScore(osScore);

        return result;
     }
}

package de.komoot.photon.searcher;

import org.assertj.core.api.AbstractAssert;

public class QueryReRankerAssert extends AbstractAssert<QueryReRankerAssert, QueryReranker> {
    public QueryReRankerAssert(QueryReranker actual) {
        super(actual, QueryReRankerAssert.class);
    }

    public static QueryReRankerAssert assertThat(String query, String language) {
        return new QueryReRankerAssert(new QueryReranker(query, language, null));
    }

    public static QueryReRankerAssert assertThat(String query, String language, String... synonymRules) {
        return new QueryReRankerAssert(new QueryReranker(query, language, null,
                QueryReranker.synonymMap(java.util.List.of(synonymRules))));
    }

    public QueryReRankerAssert ranksResultsInOrder(PhotonResult... results) {
        isNotNull();

        Double prevScore = null;
        for (int i = 0; i < results.length; ++i) {
                actual.accept(results[i]);
                double newScore = results[i].getScore();
                if (prevScore != null && newScore >= prevScore) {
                    failWithMessage("Result %s unexpectedly has larger score (%s) than result %s (%s)",
                            i, newScore, i - 1, prevScore);
                }
                prevScore = newScore;
        }

        return this;
    }

    public QueryReRankerAssert scoresResultEqualTo(double score, PhotonResult result) {
        isNotNull();

        actual.accept(result);

        if (Math.abs(result.getScore() - score) > 0.00001) {
            failWithActualExpectedAndMessage(result.getScore(), score, "Bad score");
        }

        return this;
    }
}

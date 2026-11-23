package de.komoot.photon.analyzer.normalization;

import com.ibm.icu.text.ReplaceableString;
import com.ibm.icu.text.Transliterator;

import java.util.ArrayList;
import java.util.List;

public class ICUNormalizer implements Normalizer {
    private final List<Transliterator> transliterators = new ArrayList<>();

    public ICUNormalizer(String id, List<String> rules) {
        for (var rule : rules) {
            var trans = Transliterator.createFromRules(id, rule + ";", Transliterator.FORWARD);
            // Borrowed this piece of code from Lucene. Adding a filter
            // to a rule based transliterator speeds things up A LOT.
            if (trans.getFilter() == null && trans instanceof com.ibm.icu.text.RuleBasedTransliterator) {
                final var sourceSet = trans.getSourceSet();
                if (!sourceSet.isEmpty()) {
                    trans.setFilter(sourceSet);
                }
            }
            transliterators.add(trans);
        }
    }

    @Override
    public String transform(String input) {
        var result = new ReplaceableString(input);
        var index = new Transliterator.Position(0, input.length(), 0);

        // Simplified implementation of the CompoundTransliterator loop.
        for (var trans : transliterators) {
            index.start = 0;

            if (index.limit == 0) {
                break;
            }

            trans.filteredTransliterate(result, index, false);

            if (index.start != index.limit) {
                throw new RuntimeException("Incomplete transliteration.");
            }
        }

        return result.toString();
    }
}
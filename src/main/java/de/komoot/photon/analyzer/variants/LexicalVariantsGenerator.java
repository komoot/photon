package de.komoot.photon.analyzer.variants;

import de.komoot.photon.common.SimpleTrie;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NullMarked
public class LexicalVariantsGenerator implements VariantGenerator {
    private final SimpleTrie<List<String>> replacements;

    public LexicalVariantsGenerator(SimpleTrie<List<String>> replacements) {
        assert !replacements.isEmpty();
        this.replacements = replacements;
    }

    @Override
    public Set<String> transform(Set<String> input) {
        var outset = new HashSet<String>();

        for (var word: input) {
            transformWord(outset, word);
        }

        return outset;
    }

    private void transformWord(Set<String> outset, String word) {
        var baseform = "^ " + word + " ^";
        int baselen = baseform.length();
        List<String> partials = List.of("");
        int startpos = 0;

        int pos = 0;
        boolean forceSpace = false;
        while (pos < baselen) {
            int frm = pos;
            var repl = replacements.longestPrefix(baseform, pos);
            if (repl != null) {
                pos = repl.endpos();
                String done = baseform.substring(startpos, frm);
                var newPartials = new ArrayList<String>(partials.size() * repl.value().size());
                for (var partial : partials) {
                    for (var r : repl.value()) {
                        if (!forceSpace || r.startsWith(" ")) {
                            newPartials.add(partial + done + r);
                        }
                    }
                }
                if (newPartials.size() > 128) {
                    // Too many variants produced, they are unlikely to be helpful.
                    startpos = 0;
                    break;
                }
                if (baseform.charAt(pos - 1) == ' ') {
                    --pos;
                    forceSpace = true;
                }
                startpos = pos;
                partials = newPartials;
            } else {
                ++pos;
                forceSpace = false;
            }
        }

        if (startpos == 0) {
            outset.add(word);
        } else {
            String tail = null;
            if (startpos < baselen) {
                tail = baseform.substring(startpos, baselen - 1);
            }
            for (var partial : partials) {
                String newWord;
                if (startpos < baselen) {
                    newWord = partial.substring(1) + tail;
                } else {
                    newWord = partial.substring(1, partial.length() - 1);
                }
                if (!newWord.isBlank()) {
                    outset.add(newWord.strip());
                }
            }
        }
    }
}

package de.komoot.photon.analyzer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.komoot.photon.UsageException;
import de.komoot.photon.analyzer.normalization.Normalizer;
import de.komoot.photon.common.SimpleTrie;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.regex.Pattern;

@NullMarked
public class LexicalRulesConfig extends FilterConfig {
    private record ReplacementTuple(String source, String replacement) {}

    private static class SimpleReplacementSet extends HashSet<ReplacementTuple> {

        private static final Pattern RULE_SPLIT = Pattern.compile("\\|?[=-]>");
        private static final Pattern WORDLIST_SPLIT = Pattern.compile(",");
        private static final Pattern SOURCETERM_SPLIT = Pattern.compile("^\\s*([~^]?)([^~$^]*?)([~$]?)\\s*$");

        private record SourceTerm(String term, char preflag, char postflag) {}

        private final Normalizer normalizer;

        public SimpleReplacementSet(Normalizer normalizer) {
            this.normalizer = normalizer;
        }

        void addRule(String rule) {
            var parts = RULE_SPLIT.splitWithDelimiters(rule, -1);
            if (parts.length != 3) {
                throw new UsageException("Illegal variant rule: " + rule);
            }

            boolean decompose = !parts[1].startsWith("|");
            char replOp = decompose ? parts[1].charAt(0) : parts[1].charAt(1);
            var srcTerms = parseSourceWords(parts[0]);
            var replTerms = parseReplacementWords(parts[2]);

            if (replOp == '-') {
                // Source should be kept. Add 1:1 replacement.
                for (var src : srcTerms) {
                    insertReplacements(src, src.term, decompose);
                }
            }

            for (var src : srcTerms) {
                for (var repl : replTerms) {
                    insertReplacements(src, repl, decompose);
                }
            }
        }

        public Map<String, Set<String>> getNormalisedReplacements() {
            var map = new HashMap<String, Set<String>>();

            for (var variant : this) {
                String replStr;
                if (variant.source.endsWith(" ") && variant.replacement.endsWith(" ")) {
                    replStr = variant.replacement.substring(0, variant.replacement.length() - 1);
                } else {
                    replStr = variant.replacement;
                }
                map.computeIfAbsent(variant.source, k -> new HashSet<>()).add(replStr);
            }

            return map;
        }

        private List<SourceTerm> parseSourceWords(String word) {
            var variants = new ArrayList<SourceTerm>();

            for (var term: WORDLIST_SPLIT.split(word)) {
                var match = SOURCETERM_SPLIT.matcher(term);
                if (match.find()) {
                    char pre = match.start(1) >= match.end(1) ? ' ' : term.charAt(match.start(1));
                    char post = match.start(3) >= match.end(3) ? ' ' : term.charAt(match.start(3));
                    if (pre == '~' && post == '~') {
                        throw new UsageException("Invalid variant descriptor: " + term);
                    }
                    String finalTerm = normalizer.transform(match.group(2));
                    if (!finalTerm.isBlank()) {
                        variants.add(new SourceTerm(finalTerm, pre, post));
                    }
                } else {
                    throw new UsageException("Invalid variant descriptor: " + term);
                }
            }

            return variants;
        }

        private List<String> parseReplacementWords(String word) {
            return WORDLIST_SPLIT.splitAsStream(word)
                    .map(w -> normalizer.transform(w.strip()))
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        private void insertReplacements(SourceTerm src, String repl, boolean decompose) {
            if (src.preflag == '~') {
                String postfix = src.postflag == '$' ? " ^" : " ";
                String srcTerm = src.term + postfix;
                String replTerm = repl + postfix;

                add(new ReplacementTuple(srcTerm, replTerm));
                add(new ReplacementTuple(" " + srcTerm, " " + replTerm));

                if (decompose) {
                    add(new ReplacementTuple(srcTerm, " " + replTerm));
                    add(new ReplacementTuple(" " + srcTerm, replTerm));
                }
            } else if (src.postflag == '~') {
                String prefix = src.preflag == '^' ? "^ " : " ";
                String srcTerm = prefix + src.term;
                String replTerm = prefix + repl;

                add(new ReplacementTuple(srcTerm, replTerm));
                add(new ReplacementTuple(srcTerm + " ", replTerm + " "));

                if (decompose) {
                    add(new ReplacementTuple(srcTerm, replTerm + " "));
                    add(new ReplacementTuple(srcTerm + " ", replTerm));
                }
            } else {
                String postfix = src.postflag == '$' ? " ^" : " ";
                String prefix = src.preflag == '^' ? "^ " : " ";
                add(new ReplacementTuple(prefix + src.term + postfix, prefix + repl + postfix));
            }
        }
    }

    @JsonProperty("words")
    private List<String> replacements = List.of();

    public SimpleTrie<List<String>> getReplacementTree(Normalizer normalizer) {
        var vset = new SimpleReplacementSet(normalizer);

        for (var rule: replacements) {
            vset.addRule(rule);
        }

        var trie = new SimpleTrie<List<String>>();

        for (var e : vset.getNormalisedReplacements().entrySet()) {
            trie.put(e.getKey(), new ArrayList<>(e.getValue()));
        }

        return trie;
    }
}

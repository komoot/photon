package de.komoot.photon.opensearch;

import org.jspecify.annotations.NullMarked;

import java.util.*;

/**
 * Collects strings with priorities, discarding duplicate strings while
 * keeping the highest priority.
 */
@NullMarked
public class NameCollector {
    public static final int MAXPRIO = 5;
    private final List<String>[] prioList = new List[MAXPRIO + 1];

    public NameCollector() {
        for (int i = 0; i <= MAXPRIO; ++i) {
            prioList[i] = new ArrayList<>();
        }
    }

    public void add(String term, int searchPrio) {
        assert searchPrio >= 0 && searchPrio <= MAXPRIO;
        var sanitized = term.replace("|", " ").strip();
        if (!sanitized.isEmpty()) {
            prioList[searchPrio].add(sanitized);
        }
    }

    public void addAll(Collection<String> termCollection, int searchPrio) {
        for (var term : termCollection) {
            add(term, searchPrio);
        }
    }

    public String toCollectorString() {
        var out = new StringBuilder();
        var seen = new HashSet<String>();
        for (var i = MAXPRIO; i >= 0; --i) {
            for (var e : prioList[i]) {
                if (seen.add(e)) {
                    if (!out.isEmpty()) {
                        out.append(';');
                    }
                    out.append(e).append('|').append(i);
                }
            }
        }
        return out.toString();
    }

    public static String toCollectorString(Collection<String> termCollection) {
        var out = new StringBuilder();
        var seen = new HashSet<String>();
        for (var term : termCollection) {
            var e = term.replace("|", " ").strip();
            if (seen.add(e)) {
                if (!out.isEmpty()) {
                    out.append(';');
                }
                out.append(e).append("|1");
            }
        }
        return out.toString();
    }
}

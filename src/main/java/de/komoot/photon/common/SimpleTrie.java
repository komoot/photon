package de.komoot.photon.common;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static java.lang.Integer.min;

@NullMarked
public class SimpleTrie<T> implements Iterable<SimpleTrie.Entry<T>> {
    public record Entry<T>(String key, T value){}

    public record Match<T>(T value, int endpos) {}

    private Map<Character, SimpleTrie<T> > children = new HashMap<>();
    private String prefix = "";
    @Nullable private T payload;

    public boolean isEmpty() {
        return children.isEmpty() && payload == null;
    }

    public void put(String key, T value) {
        if (key.isEmpty()) {
            throw new RuntimeException("Trie cannot have empty key.");
        }
        put_internal(key, 0, value);
    }

    private void put_internal(String key, int pos, T value) {
        if (pos >= key.length()) {
            payload = value;
        } else if (children.isEmpty() && payload == null) {
            prefix = key.substring(pos);
            payload = value;
        } else {
            int prefixPos = 0;
            int max_prefix_len = min(prefix.length(), key.length() - pos);
            while (prefixPos < max_prefix_len) {
                if (prefix.charAt(prefixPos) != key.charAt(pos + prefixPos)) {
                    break;
                }
                ++prefixPos;
            }

            if (prefixPos >= prefix.length()) {
                if (pos + prefixPos >= key.length()) {
                    payload = value;
                } else {
                    children.computeIfAbsent(key.charAt(pos + prefixPos), c -> new SimpleTrie<>())
                            .put_internal(key, pos + prefixPos + 1, value);
                }
            } else {
                var newChild = new SimpleTrie<T>();
                newChild.children = children;
                newChild.payload = payload;
                newChild.prefix = prefix.substring(prefixPos + 1);

                children = new HashMap<>();
                children.put(prefix.charAt(prefixPos), newChild);
                prefix = prefix.substring(0, prefixPos);

                if (prefixPos + pos >= key.length()) {
                    payload = value;
                } else {
                    payload = null;
                    var newTrie = new SimpleTrie<T>();
                    newTrie.put_internal(key, pos + prefixPos + 1, value);
                    children.put(key.charAt(pos + prefixPos), newTrie);
                }
            }
        }
    }

    @Nullable public Match<T> longestPrefix(String word, int start) {
        var wordLen = word.length();
        var cur = this;
        var pos = start;

        Match<T> result = null;

        while (cur != null) {
            if (!cur.prefix.isEmpty()) {
                if (!word.startsWith(cur.prefix, pos)) {
                    return result;
                }
                pos += cur.prefix.length();
            }

            if (cur.payload != null) {
                result = new Match<>(cur.payload, pos);
            }

            if (pos >= wordLen) {
                return result;
            }

            cur = cur.children.get(word.charAt(pos));
            ++pos;
        }

        return result;
    }

    public Iterator<Entry<T>> iterator() {
        return new TrieIterator<>(this, "");
    }

    private static class TrieIterator<T> implements Iterator<Entry<T>> {
        private final Iterator<Map.Entry<Character, SimpleTrie<T>>> childIterator;
        String prefix;
        @Nullable TrieIterator<T> current;
        private @Nullable Entry<T> nextItem;

        public TrieIterator(SimpleTrie<T> root, String prefix) {
            childIterator = root.children.entrySet().iterator();
            this.prefix = prefix + root.prefix;
            if (root.payload != null) {
                nextItem = new Entry<>(this.prefix, root.payload);
            } else {
                nextItem = null;
            }
        }

        @Override
        public boolean hasNext() {
            while (nextItem == null) {
                if (current == null) {
                    if (!childIterator.hasNext()) {
                        return false;
                    }

                    var nextEntry = childIterator.next();
                    current = new TrieIterator<>(nextEntry.getValue(), prefix + nextEntry.getKey());
                }

                if (current.hasNext()) {
                    nextItem = current.next();
                } else {
                    current = null;
                }
            }

            return true;
        }

        @Override
        public Entry<T> next() {
            if (nextItem == null) {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                assert nextItem != null;
            }
            var out = nextItem;
            nextItem = null;

            return out;
        }
    }
}
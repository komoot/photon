/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.komoot.photon.lucene.analysis.compound;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;

/**
 * Base class for decomposition token filters.
 */
public abstract class CompoundWordTokenFilterBase extends TokenFilter {
    /**
     * The default for minimal word length that gets decomposed
     */
    public static final int DEFAULT_MIN_WORD_SIZE = 5;

    /**
     * The default for minimal length of subwords that get propagated to the
     * output of this filter
     */
    public static final int DEFAULT_MIN_SUBWORD_SIZE = 2;

    /**
     * The default for maximal length of subwords that get propagated to the
     * output of this filter
     */
    public static final int DEFAULT_MAX_SUBWORD_SIZE = 15;

    protected final CharArraySet dictionary;
    protected final LinkedList<CompoundToken> tokens;
    protected final int minWordSize;
    protected final int minSubwordSize;
    protected final int maxSubwordSize;
    protected final boolean onlyLongestMatch;

    protected final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    protected final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    protected final PositionIncrementAttribute posIncAtt = addAttribute(
                    PositionIncrementAttribute.class);
    private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);

    private State current;

    protected CompoundWordTokenFilterBase(TokenStream input, CharArraySet dictionary,
                    boolean onlyLongestMatch) {
        this(input, dictionary, DEFAULT_MIN_WORD_SIZE, DEFAULT_MIN_SUBWORD_SIZE,
                        DEFAULT_MAX_SUBWORD_SIZE, onlyLongestMatch);
    }

    protected CompoundWordTokenFilterBase(TokenStream input, CharArraySet dictionary) {
        this(input, dictionary, DEFAULT_MIN_WORD_SIZE, DEFAULT_MIN_SUBWORD_SIZE,
                        DEFAULT_MAX_SUBWORD_SIZE, false);
    }

    protected CompoundWordTokenFilterBase(TokenStream input, CharArraySet dictionary,
                    int minWordSize, int minSubwordSize, int maxSubwordSize,
                    boolean onlyLongestMatch) {
        super(input);
        this.tokens = new LinkedList<>();
        if (minWordSize < 0) {
            throw new IllegalArgumentException("minWordSize cannot be negative");
        }
        this.minWordSize = minWordSize;
        if (minSubwordSize < 0) {
            throw new IllegalArgumentException("minSubwordSize cannot be negative");
        }
        this.minSubwordSize = minSubwordSize;
        if (maxSubwordSize < 0) {
            throw new IllegalArgumentException("maxSubwordSize cannot be negative");
        }
        this.maxSubwordSize = maxSubwordSize;
        this.onlyLongestMatch = onlyLongestMatch;
        this.dictionary = dictionary;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (!tokens.isEmpty()) {
            return incrementSubword();
        }

        current = null; // not really needed, but for safety
        if (input.incrementToken()) {
            // Only words longer than minWordSize get processed
            if (termAtt.length() >= this.minWordSize) {
                decompose();
                // only capture the state if we really need it for producing new
                // tokens
                if (!tokens.isEmpty()) {
                    current = captureState();
                    return incrementSubword();
                }
            }
            // return original token, only if there are no subwords, otherwise
            // decompounder must have added it to tokens with updated position
            // length
            return originalToken();
        } else {
            return false;
        }
    }

    protected boolean originalToken() {
        return true;
    }

    private boolean incrementSubword() {
        assert current != null;
        CompoundToken token = tokens.removeFirst();
        restoreState(current); // keep all other attributes untouched
        termAtt.setEmpty().append(token.txt);
        offsetAtt.setOffset(token.startOffset, token.endOffset);
        posIncAtt.setPositionIncrement(token.positionIncrement);
        posLenAtt.setPositionLength(token.positionLength);
        return true;
    }

    /**
     * Decomposes the current {@link #termAtt} and places {@link CompoundToken}
     * instances in the {@link #tokens} list. The original token may not be
     * placed in the list, as it is automatically passed through this filter.
     */
    protected abstract void decompose();

    @Override
    public void reset() throws IOException {
        super.reset();
        tokens.clear();
        current = null;
    }

    /**
     * Helper class to hold decompounded token information
     */
    protected class CompoundToken {
        public final CharSequence txt;
        public final int startOffset, endOffset, positionIncrement, positionLength;

        /**
         * Construct the compound token based on a slice of the current
         * {@link CompoundWordTokenFilterBase#termAtt}.
         */
        public CompoundToken(int offset, int length, int positionIncrement) {
            this.txt = CompoundWordTokenFilterBase.this.termAtt.subSequence(offset,
                            offset + length);
            this.positionIncrement = positionIncrement;

            // offsets/posLength of the original word for DictionaryDecompounder
            this.startOffset = CompoundWordTokenFilterBase.this.offsetAtt.startOffset();
            this.endOffset = CompoundWordTokenFilterBase.this.offsetAtt.endOffset();
            this.positionLength = CompoundWordTokenFilterBase.this.posLenAtt.getPositionLength();
        }

        /**
         * Construct the compound token based on a slice of the current
         * {@link CompoundWordTokenFilterBase#termAtt}.
         */
        public CompoundToken(int offset, int length, int positionIncrement, int positionLength) {
            this.txt = CompoundWordTokenFilterBase.this.termAtt.subSequence(offset,
                            offset + length);

            // offsets of the token, not the original word, so highlighting is
            // possible
            this.startOffset = CompoundWordTokenFilterBase.this.offsetAtt.startOffset() + offset;
            int originalOffset = CompoundWordTokenFilterBase.this.offsetAtt.endOffset();
            this.endOffset = Math.min(this.startOffset + length, originalOffset);
            this.positionIncrement = positionIncrement;
            this.positionLength = positionLength;
        }

    }
}

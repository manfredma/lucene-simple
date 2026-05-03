package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.index.internal.PostingList;

import java.util.*;

public class PhraseQuery implements Query {
    private final String field;
    private final List<Term> terms;

    private PhraseQuery(String field, List<Term> terms) {
        this.field = field;
        this.terms = Collections.unmodifiableList(terms);
    }

    public List<Term> getTerms() { return terms; }

    @Override
    public Set<Integer> execute(IndexReader reader) {
        if (terms.isEmpty()) return Collections.emptySet();

        PostingList firstPl = reader.getPostingList(terms.get(0));
        if (firstPl == null) return Collections.emptySet();
        Set<Integer> candidates = new HashSet<>(firstPl.getDocIds());

        Set<Integer> result = new HashSet<>();
        for (int docId : candidates) {
            if (matchesPhrase(docId, reader)) {
                result.add(docId);
            }
        }
        return result;
    }

    private boolean matchesPhrase(int docId, IndexReader reader) {
        PostingList firstPl = reader.getPostingList(terms.get(0));
        if (firstPl == null) return false;
        List<Integer> startPositions = firstPl.getPositions(docId);

        for (int startPos : startPositions) {
            boolean phraseMatch = true;
            for (int i = 1; i < terms.size(); i++) {
                PostingList pl = reader.getPostingList(terms.get(i));
                if (pl == null || !pl.getPositions(docId).contains(startPos + i)) {
                    phraseMatch = false;
                    break;
                }
            }
            if (phraseMatch) return true;
        }
        return false;
    }

    public static class Builder {
        private String field;
        private final List<Term> terms = new ArrayList<>();

        public Builder setField(String field) {
            this.field = field;
            return this;
        }

        public Builder add(Term term) {
            terms.add(term);
            return this;
        }

        public PhraseQuery build() {
            return new PhraseQuery(field, new ArrayList<>(terms));
        }
    }
}

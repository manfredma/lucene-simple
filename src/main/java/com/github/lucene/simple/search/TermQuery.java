package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.index.internal.PostingList;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TermQuery implements Query {
    private final Term term;

    public TermQuery(Term term) {
        this.term = term;
    }

    public Term getTerm() { return term; }

    @Override
    public Set<Integer> execute(IndexReader reader) {
        PostingList pl = reader.getPostingList(term);
        if (pl == null) return Collections.emptySet();
        return new HashSet<>(pl.getDocIds());
    }
}

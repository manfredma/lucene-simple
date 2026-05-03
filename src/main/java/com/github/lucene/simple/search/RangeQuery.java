package com.github.lucene.simple.search;

import com.github.lucene.simple.index.DirectoryReader;
import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.index.internal.PostingList;

import java.util.HashSet;
import java.util.Set;

public class RangeQuery implements Query {
    private final String field;
    private final String lowerTerm;
    private final String upperTerm;

    public RangeQuery(String field, String lowerTerm, String upperTerm) {
        this.field = field;
        this.lowerTerm = lowerTerm;
        this.upperTerm = upperTerm;
    }

    @Override
    public Set<Integer> execute(IndexReader reader) {
        Set<Integer> result = new HashSet<>();
        if (!(reader instanceof DirectoryReader)) return result;
        DirectoryReader dr = (DirectoryReader) reader;
        for (Term term : dr.getTermDictionary().terms()) {
            if (!term.field().equals(field)) continue;
            String text = term.text();
            if (lowerTerm != null && text.compareTo(lowerTerm) < 0) continue;
            if (upperTerm != null && text.compareTo(upperTerm) > 0) continue;
            PostingList pl = reader.getPostingList(term);
            if (pl != null) result.addAll(pl.getDocIds());
        }
        return result;
    }
}

package com.github.lucene.simple.index.internal;

import com.github.lucene.simple.index.Term;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TermDictionary implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Term, PostingList> dictionary = new HashMap<>();

    public void addPosting(Term term, int docId, int position) {
        dictionary.computeIfAbsent(term, k -> new PostingList()).addPosting(docId, position);
    }

    public PostingList getPostingList(Term term) {
        return dictionary.get(term);
    }

    public int getDocFreq(Term term) {
        PostingList pl = dictionary.get(term);
        return pl != null ? pl.getDocFreq() : 0;
    }

    public Set<Term> terms() {
        return Collections.unmodifiableSet(dictionary.keySet());
    }
}

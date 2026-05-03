package com.github.lucene.simple.index.internal;

import java.io.Serializable;
import java.util.*;

public class PostingList implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, List<Integer>> postings = new LinkedHashMap<>();

    public void addPosting(int docId, int position) {
        postings.computeIfAbsent(docId, k -> new ArrayList<>()).add(position);
    }

    public Set<Integer> getDocIds() {
        return Collections.unmodifiableSet(postings.keySet());
    }

    public List<Integer> getPositions(int docId) {
        List<Integer> pos = postings.get(docId);
        return pos != null ? Collections.unmodifiableList(pos) : Collections.emptyList();
    }

    public int getTermFreq(int docId) {
        List<Integer> pos = postings.get(docId);
        return pos != null ? pos.size() : 0;
    }

    public int getDocFreq() {
        return postings.size();
    }
}

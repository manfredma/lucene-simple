package com.github.lucene.simple.index;

import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.index.internal.PostingList;

public abstract class IndexReader {
    public abstract int numDocs();
    public abstract int maxDoc();
    public abstract Document document(int docId);
    public abstract PostingList getPostingList(Term term);
    public abstract int getDocFreq(Term term);
}

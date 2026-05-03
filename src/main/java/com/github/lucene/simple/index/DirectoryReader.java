package com.github.lucene.simple.index;

import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.index.internal.PostingList;
import com.github.lucene.simple.index.internal.SegmentInfo;
import com.github.lucene.simple.index.internal.TermDictionary;
import com.github.lucene.simple.store.Directory;

public class DirectoryReader extends IndexReader {
    private final TermDictionary termDictionary;
    private final SegmentInfo segmentInfo;

    private DirectoryReader(TermDictionary termDictionary, SegmentInfo segmentInfo) {
        this.termDictionary = termDictionary;
        this.segmentInfo = segmentInfo;
    }

    public static DirectoryReader open(Directory directory) {
        TermDictionary td = directory.getTermDictionary();
        SegmentInfo si = directory.getSegmentInfo();
        if (td == null) td = new TermDictionary();
        if (si == null) si = new SegmentInfo();
        return new DirectoryReader(td, si);
    }

    @Override
    public int numDocs() { return segmentInfo.numDocs(); }

    @Override
    public int maxDoc() { return segmentInfo.maxDoc(); }

    @Override
    public Document document(int docId) { return segmentInfo.getDocument(docId); }

    @Override
    public PostingList getPostingList(Term term) { return termDictionary.getPostingList(term); }

    @Override
    public int getDocFreq(Term term) { return termDictionary.getDocFreq(term); }

    public TermDictionary getTermDictionary() { return termDictionary; }
}

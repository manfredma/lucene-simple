package com.github.lucene.simple.store;

import com.github.lucene.simple.index.internal.SegmentInfo;
import com.github.lucene.simple.index.internal.TermDictionary;

public class RAMDirectory implements Directory {
    private TermDictionary termDictionary;
    private SegmentInfo segmentInfo;

    @Override
    public TermDictionary getTermDictionary() { return termDictionary; }

    @Override
    public void setTermDictionary(TermDictionary termDictionary) { this.termDictionary = termDictionary; }

    @Override
    public SegmentInfo getSegmentInfo() { return segmentInfo; }

    @Override
    public void setSegmentInfo(SegmentInfo segmentInfo) { this.segmentInfo = segmentInfo; }

    @Override
    public void clear() {
        termDictionary = null;
        segmentInfo = null;
    }
}

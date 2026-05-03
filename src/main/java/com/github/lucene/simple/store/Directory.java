package com.github.lucene.simple.store;

import com.github.lucene.simple.index.internal.SegmentInfo;
import com.github.lucene.simple.index.internal.TermDictionary;

public interface Directory {
    TermDictionary getTermDictionary();
    void setTermDictionary(TermDictionary termDictionary);
    SegmentInfo getSegmentInfo();
    void setSegmentInfo(SegmentInfo segmentInfo);
    void clear();
}

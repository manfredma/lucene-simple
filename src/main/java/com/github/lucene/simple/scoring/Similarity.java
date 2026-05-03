package com.github.lucene.simple.scoring;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;

public interface Similarity {
    float score(Term term, int docId, IndexReader reader);
    float tf(int freq);
    float idf(int numDocs, int docFreq);
}

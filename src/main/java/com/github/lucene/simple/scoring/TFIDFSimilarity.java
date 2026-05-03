package com.github.lucene.simple.scoring;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.index.internal.PostingList;

public class TFIDFSimilarity implements Similarity {

    @Override
    public float score(Term term, int docId, IndexReader reader) {
        PostingList pl = reader.getPostingList(term);
        if (pl == null) return 0f;
        int freq = pl.getTermFreq(docId);
        if (freq == 0) return 0f;
        int numDocs = reader.numDocs();
        int docFreq = reader.getDocFreq(term);
        return tf(freq) * idf(numDocs, docFreq);
    }

    @Override
    public float tf(int freq) {
        return (float) Math.sqrt(freq);
    }

    @Override
    public float idf(int numDocs, int docFreq) {
        return (float) (Math.log((numDocs + 1.0) / (docFreq + 1.0)) + 1.0);
    }
}

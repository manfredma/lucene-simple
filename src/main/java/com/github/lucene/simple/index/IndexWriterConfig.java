package com.github.lucene.simple.index;

import com.github.lucene.simple.analysis.Analyzer;
import com.github.lucene.simple.scoring.Similarity;

public class IndexWriterConfig {
    private final Analyzer analyzer;
    private Similarity similarity;

    public IndexWriterConfig(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Analyzer getAnalyzer() { return analyzer; }
    public Similarity getSimilarity() { return similarity; }
    public IndexWriterConfig setSimilarity(Similarity similarity) {
        this.similarity = similarity;
        return this;
    }
}

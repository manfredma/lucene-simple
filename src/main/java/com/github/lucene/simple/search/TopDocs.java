package com.github.lucene.simple.search;

public class TopDocs {
    private final int totalHits;
    private final ScoreDoc[] scoreDocs;

    public TopDocs(int totalHits, ScoreDoc[] scoreDocs) {
        this.totalHits = totalHits;
        this.scoreDocs = scoreDocs;
    }

    public int getTotalHits() { return totalHits; }
    public ScoreDoc[] getScoreDocs() { return scoreDocs; }
}

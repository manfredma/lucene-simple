package com.github.lucene.simple.search;

public class ScoreDoc {
    private final int docId;
    private final float score;

    public ScoreDoc(int docId, float score) {
        this.docId = docId;
        this.score = score;
    }

    public int getDocId() { return docId; }
    public float getScore() { return score; }

    @Override
    public String toString() {
        return "ScoreDoc{docId=" + docId + ", score=" + score + "}";
    }
}

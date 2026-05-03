package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.scoring.Similarity;
import com.github.lucene.simple.scoring.TFIDFSimilarity;

import java.util.*;

public class IndexSearcher {
    private final IndexReader reader;
    private final Similarity similarity;

    public IndexSearcher(IndexReader reader) {
        this.reader = reader;
        this.similarity = new TFIDFSimilarity();
    }

    public IndexSearcher(IndexReader reader, Similarity similarity) {
        this.reader = reader;
        this.similarity = similarity;
    }

    public TopDocs search(Query query, int n) {
        Set<Integer> matchingDocIds = query.execute(reader);
        int totalHits = matchingDocIds.size();

        List<ScoreDoc> scoreDocs = new ArrayList<>();
        for (int docId : matchingDocIds) {
            float score = computeScore(query, docId);
            scoreDocs.add(new ScoreDoc(docId, score));
        }

        scoreDocs.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        int size = Math.min(n, scoreDocs.size());
        ScoreDoc[] topN = scoreDocs.subList(0, size).toArray(new ScoreDoc[0]);
        return new TopDocs(totalHits, topN);
    }

    private float computeScore(Query query, int docId) {
        if (query instanceof TermQuery) {
            return similarity.score(((TermQuery) query).getTerm(), docId, reader);
        }
        if (query instanceof BooleanQuery) {
            float total = 0f;
            for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
                if (clause.getOccur() != BooleanClause.Occur.MUST_NOT) {
                    total += computeScore(clause.getQuery(), docId);
                }
            }
            return total;
        }
        if (query instanceof PhraseQuery) {
            PhraseQuery pq = (PhraseQuery) query;
            float total = 0f;
            int count = 0;
            for (Term term : pq.getTerms()) {
                total += similarity.score(term, docId, reader);
                count++;
            }
            return count > 0 ? total / count : 0f;
        }
        return 1.0f;
    }
}

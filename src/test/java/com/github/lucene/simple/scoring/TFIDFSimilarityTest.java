package com.github.lucene.simple.scoring;

import com.github.lucene.simple.analysis.StandardAnalyzer;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.*;
import com.github.lucene.simple.store.RAMDirectory;
import org.junit.Test;
import static org.junit.Assert.*;

public class TFIDFSimilarityTest {

    @Test
    public void testScoreHigherForMoreFrequent() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));

        Document doc0 = new Document();
        doc0.add(new Field("content", "lucene search", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc0);

        Document doc1 = new Document();
        doc1.add(new Field("content", "lucene lucene lucene engine", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc1);

        writer.commit();
        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        TFIDFSimilarity similarity = new TFIDFSimilarity();
        Term term = new Term("content", "lucene");

        float score0 = similarity.score(term, 0, reader);
        float score1 = similarity.score(term, 1, reader);

        assertTrue("出现更多次的文档评分应更高: score0=" + score0 + ", score1=" + score1,
                score1 > score0);
    }

    @Test
    public void testTfFormula() {
        TFIDFSimilarity sim = new TFIDFSimilarity();
        assertEquals((float) Math.sqrt(4), sim.tf(4), 0.001f);
        assertEquals((float) Math.sqrt(1), sim.tf(1), 0.001f);
    }

    @Test
    public void testIdfFormula() {
        TFIDFSimilarity sim = new TFIDFSimilarity();
        float expected = (float) (Math.log((10.0 + 1) / (2.0 + 1)) + 1);
        assertEquals(expected, sim.idf(10, 2), 0.001f);
    }
}

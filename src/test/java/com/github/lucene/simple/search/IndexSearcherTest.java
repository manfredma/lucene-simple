package com.github.lucene.simple.search;

import com.github.lucene.simple.analysis.StandardAnalyzer;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.*;
import com.github.lucene.simple.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class IndexSearcherTest {

    private IndexSearcher searcher;
    private IndexReader reader;

    @Before
    public void setUp() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));

        Document doc0 = new Document();
        doc0.add(new Field("title", "Lucene search engine", FieldType.TEXT_INDEXED_STORED));
        doc0.add(new Field("id", "0", FieldType.STRING_INDEXED_STORED));
        writer.addDocument(doc0);

        Document doc1 = new Document();
        doc1.add(new Field("title", "Elasticsearch search platform", FieldType.TEXT_INDEXED_STORED));
        doc1.add(new Field("id", "1", FieldType.STRING_INDEXED_STORED));
        writer.addDocument(doc1);

        Document doc2 = new Document();
        doc2.add(new Field("title", "Apache Solr full text search", FieldType.TEXT_INDEXED_STORED));
        doc2.add(new Field("id", "2", FieldType.STRING_INDEXED_STORED));
        writer.addDocument(doc2);

        writer.commit();
        writer.close();

        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
    }

    @Test
    public void testTermQuery() {
        TopDocs results = searcher.search(new TermQuery(new Term("title", "search")), 10);
        assertEquals(3, results.getTotalHits());
    }

    @Test
    public void testBooleanQueryMust() {
        BooleanQuery query = new BooleanQuery.Builder()
                .add(new TermQuery(new Term("title", "search")), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term("title", "lucene")), BooleanClause.Occur.MUST)
                .build();
        TopDocs results = searcher.search(query, 10);
        assertEquals(1, results.getTotalHits());
    }

    @Test
    public void testBooleanQueryMustNot() {
        BooleanQuery query = new BooleanQuery.Builder()
                .add(new TermQuery(new Term("title", "search")), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term("title", "lucene")), BooleanClause.Occur.MUST_NOT)
                .build();
        TopDocs results = searcher.search(query, 10);
        assertEquals(2, results.getTotalHits());
    }

    @Test
    public void testPhraseQuery() {
        PhraseQuery query = new PhraseQuery.Builder()
                .setField("title")
                .add(new Term("title", "lucene"))
                .add(new Term("title", "search"))
                .build();
        TopDocs results = searcher.search(query, 10);
        assertEquals(1, results.getTotalHits());
    }

    @Test
    public void testTopNResults() {
        TopDocs results = searcher.search(new TermQuery(new Term("title", "search")), 2);
        assertEquals(3, results.getTotalHits());
        assertEquals(2, results.getScoreDocs().length);
    }

    @Test
    public void testResultsOrderedByScore() {
        TopDocs results = searcher.search(new TermQuery(new Term("title", "lucene")), 10);
        assertEquals(1, results.getTotalHits());
        assertEquals(0, results.getScoreDocs()[0].getDocId());
    }
}

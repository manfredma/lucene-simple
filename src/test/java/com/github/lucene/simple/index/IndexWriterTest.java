package com.github.lucene.simple.index;

import com.github.lucene.simple.analysis.StandardAnalyzer;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.internal.PostingList;
import com.github.lucene.simple.store.RAMDirectory;
import org.junit.Test;
import static org.junit.Assert.*;

public class IndexWriterTest {

    @Test
    public void testAddDocumentAndRead() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(dir, config);

        Document doc = new Document();
        doc.add(new Field("title", "Lucene search engine", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc);
        writer.commit();
        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        assertEquals(1, reader.numDocs());

        Document stored = reader.document(0);
        assertNotNull(stored);
        assertEquals("Lucene search engine", stored.getField("title").stringValue());
    }

    @Test
    public void testIndexContainsTerms() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));

        Document doc = new Document();
        doc.add(new Field("content", "hello world", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc);
        writer.commit();
        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        PostingList pl = reader.getPostingList(new Term("content", "hello"));
        assertNotNull(pl);
        assertTrue(pl.getDocIds().contains(0));
    }

    @Test
    public void testDeleteDocuments() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));

        Document doc1 = new Document();
        doc1.add(new Field("id", "1", FieldType.STRING_INDEXED_STORED));
        doc1.add(new Field("content", "hello", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc1);

        Document doc2 = new Document();
        doc2.add(new Field("id", "2", FieldType.STRING_INDEXED_STORED));
        doc2.add(new Field("content", "world", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc2);
        writer.commit();

        writer.deleteDocuments(new Term("id", "1"));
        writer.commit();
        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        assertEquals(1, reader.numDocs());
    }
}

package com.github.lucene.simple.store;

import com.github.lucene.simple.analysis.StandardAnalyzer;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import static org.junit.Assert.*;

public class FSDirectoryTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testSaveAndLoad() throws Exception {
        File indexFile = new File(tmpFolder.getRoot(), "test.idx");

        FSDirectory dir = FSDirectory.open(indexFile.toPath());
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));
        Document doc = new Document();
        doc.add(new Field("title", "save and load test", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc);
        writer.close();
        dir.save();

        FSDirectory dir2 = FSDirectory.open(indexFile.toPath());
        dir2.load();
        IndexReader reader = DirectoryReader.open(dir2);
        assertEquals(1, reader.numDocs());
        assertEquals("save and load test", reader.document(0).getField("title").stringValue());
    }
}

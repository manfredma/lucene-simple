package com.github.lucene.simple.document;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class DocumentTest {

    @Test
    public void testAddAndGetField() {
        Document doc = new Document();
        doc.add(new Field("title", "Hello World", FieldType.TEXT_INDEXED_STORED));
        Field f = doc.getField("title");
        assertNotNull(f);
        assertEquals("Hello World", f.stringValue());
    }

    @Test
    public void testMultipleFields() {
        Document doc = new Document();
        doc.add(new Field("title", "Lucene", FieldType.TEXT_INDEXED_STORED));
        doc.add(new Field("body", "search engine", FieldType.TEXT_INDEXED_STORED));
        List<Field> fields = doc.getFields();
        assertEquals(2, fields.size());
    }

    @Test
    public void testFieldType() {
        FieldType type = FieldType.TEXT_INDEXED_STORED;
        assertTrue(type.isIndexed());
        assertTrue(type.isStored());
        assertTrue(type.isTokenized());

        FieldType storedOnly = FieldType.STORED_ONLY;
        assertFalse(storedOnly.isIndexed());
        assertTrue(storedOnly.isStored());
    }
}

package com.github.lucene.simple.index;

import com.github.lucene.simple.index.internal.PostingList;
import com.github.lucene.simple.index.internal.TermDictionary;
import org.junit.Test;
import static org.junit.Assert.*;

public class TermDictionaryTest {

    @Test
    public void testAddAndGetPosting() {
        TermDictionary dict = new TermDictionary();
        Term term = new Term("content", "lucene");
        dict.addPosting(term, 0, 0);
        dict.addPosting(term, 1, 0);

        PostingList pl = dict.getPostingList(term);
        assertNotNull(pl);
        assertEquals(2, pl.getDocIds().size());
        assertTrue(pl.getDocIds().contains(0));
        assertTrue(pl.getDocIds().contains(1));
    }

    @Test
    public void testDocFreq() {
        TermDictionary dict = new TermDictionary();
        Term term = new Term("content", "search");
        dict.addPosting(term, 0, 0);
        dict.addPosting(term, 0, 1);
        dict.addPosting(term, 2, 0);

        assertEquals(2, dict.getDocFreq(term));
    }

    @Test
    public void testTermEquals() {
        Term t1 = new Term("content", "hello");
        Term t2 = new Term("content", "hello");
        Term t3 = new Term("title", "hello");
        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
    }
}

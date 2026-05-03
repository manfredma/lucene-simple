package com.github.lucene.simple.analysis;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class StandardAnalyzerTest {

    private final Analyzer analyzer = new StandardAnalyzer();

    @Test
    public void testLowercaseAndSplit() {
        List<Token> tokens = analyzer.analyze("content", "Hello World");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
        assertEquals("world", tokens.get(1).getText());
    }

    @Test
    public void testPunctuationRemoved() {
        List<Token> tokens = analyzer.analyze("content", "hello, world!");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
        assertEquals("world", tokens.get(1).getText());
    }

    @Test
    public void testStopWords() {
        List<Token> tokens = analyzer.analyze("content", "the quick brown fox");
        assertEquals(3, tokens.size());
        assertEquals("quick", tokens.get(0).getText());
    }

    @Test
    public void testEmptyInput() {
        List<Token> tokens = analyzer.analyze("content", "");
        assertTrue(tokens.isEmpty());
    }
}

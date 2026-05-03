package com.github.lucene.simple.analysis;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ChineseAnalyzerTest {

    private final Analyzer analyzer = new ChineseAnalyzer();

    @Test
    public void testChineseTokenization() {
        List<Token> tokens = analyzer.analyze("content", "中文分词测试");
        assertTrue("应切出至少1个词", tokens.size() >= 1);
        for (Token t : tokens) {
            assertFalse(t.getText().isEmpty());
        }
    }

    @Test
    public void testEnglishFallback() {
        List<Token> tokens = analyzer.analyze("content", "hello world");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
    }

    @Test
    public void testMixedContent() {
        List<Token> tokens = analyzer.analyze("content", "Lucene搜索引擎");
        assertTrue("混合内容应切出多个词", tokens.size() >= 2);
    }
}

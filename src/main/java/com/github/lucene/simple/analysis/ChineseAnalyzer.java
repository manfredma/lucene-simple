package com.github.lucene.simple.analysis;

import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class ChineseAnalyzer implements Analyzer {

    private final StandardAnalyzer fallback = new StandardAnalyzer();

    @Override
    public List<Token> analyze(String fieldName, String text) {
        if (text == null || text.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        if (!containsChinese(text)) {
            return fallback.analyze(fieldName, text);
        }
        List<Token> tokens = new ArrayList<>();
        try {
            IKSegmenter segmenter = new IKSegmenter(new StringReader(text), true);
            Lexeme lexeme;
            while ((lexeme = segmenter.next()) != null) {
                String word = lexeme.getLexemeText().toLowerCase().trim();
                if (!word.isEmpty()) {
                    tokens.add(new Token(word, lexeme.getBeginPosition(), lexeme.getEndPosition()));
                }
            }
        } catch (Exception e) {
            return fallback.analyze(fieldName, text);
        }
        return tokens;
    }

    private boolean containsChinese(String text) {
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                return true;
            }
        }
        return false;
    }
}

package com.github.lucene.simple.analysis;

import java.util.*;

public class StandardAnalyzer implements Analyzer {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "shall",
        "should", "may", "might", "must", "can", "could", "of", "in", "on",
        "at", "to", "for", "with", "by", "from", "and", "or", "but", "not"
    ));

    @Override
    public List<Token> analyze(String fieldName, String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int len = text.length();
        while (i < len) {
            while (i < len && !Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
            if (i >= len) break;
            int start = i;
            while (i < len && Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
            String word = text.substring(start, i).toLowerCase();
            if (!STOP_WORDS.contains(word) && !word.isEmpty()) {
                tokens.add(new Token(word, start, i));
            }
        }
        return tokens;
    }
}

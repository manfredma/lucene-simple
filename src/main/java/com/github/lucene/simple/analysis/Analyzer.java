package com.github.lucene.simple.analysis;

import java.util.List;

public interface Analyzer {
    List<Token> analyze(String fieldName, String text);
}

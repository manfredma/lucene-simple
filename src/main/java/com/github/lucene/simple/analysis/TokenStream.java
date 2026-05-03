package com.github.lucene.simple.analysis;

import java.util.Iterator;

public interface TokenStream extends Iterator<Token> {
    boolean hasNext();
    Token next();
}

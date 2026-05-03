package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;
import java.util.Set;

public interface Query {
    Set<Integer> execute(IndexReader reader);
}

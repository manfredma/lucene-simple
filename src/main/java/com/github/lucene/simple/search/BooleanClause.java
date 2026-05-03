package com.github.lucene.simple.search;

public class BooleanClause {
    public enum Occur { MUST, SHOULD, MUST_NOT }

    private final Query query;
    private final Occur occur;

    public BooleanClause(Query query, Occur occur) {
        this.query = query;
        this.occur = occur;
    }

    public Query getQuery() { return query; }
    public Occur getOccur() { return occur; }
}

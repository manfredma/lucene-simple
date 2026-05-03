package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;

import java.util.*;

public class BooleanQuery implements Query {
    private final List<BooleanClause> clauses;

    private BooleanQuery(List<BooleanClause> clauses) {
        this.clauses = Collections.unmodifiableList(clauses);
    }

    public List<BooleanClause> clauses() { return clauses; }

    @Override
    public Set<Integer> execute(IndexReader reader) {
        Set<Integer> result = null;
        Set<Integer> mustNot = new HashSet<>();

        for (BooleanClause clause : clauses) {
            Set<Integer> docs = clause.getQuery().execute(reader);
            switch (clause.getOccur()) {
                case MUST:
                    if (result == null) result = new HashSet<>(docs);
                    else result.retainAll(docs);
                    break;
                case SHOULD:
                    if (result == null) result = new HashSet<>(docs);
                    else result.addAll(docs);
                    break;
                case MUST_NOT:
                    mustNot.addAll(docs);
                    break;
            }
        }
        if (result == null) result = new HashSet<>();
        result.removeAll(mustNot);
        return result;
    }

    public static class Builder {
        private final List<BooleanClause> clauses = new ArrayList<>();

        public Builder add(Query query, BooleanClause.Occur occur) {
            clauses.add(new BooleanClause(query, occur));
            return this;
        }

        public BooleanQuery build() {
            return new BooleanQuery(new ArrayList<>(clauses));
        }
    }
}

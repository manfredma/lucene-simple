package com.github.lucene.simple.index;

import java.io.Serializable;
import java.util.Objects;

public final class Term implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String field;
    private final String text;

    public Term(String field, String text) {
        this.field = field;
        this.text = text;
    }

    public String field() { return field; }
    public String text() { return text; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Term)) return false;
        Term term = (Term) o;
        return Objects.equals(field, term.field) && Objects.equals(text, term.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, text);
    }

    @Override
    public String toString() {
        return field + ":" + text;
    }
}

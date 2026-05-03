package com.github.lucene.simple.document;

import java.io.Serializable;

public final class FieldType implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final FieldType TEXT_INDEXED_STORED = new FieldType(true, true, true);
    public static final FieldType TEXT_INDEXED_NOT_STORED = new FieldType(true, false, true);
    public static final FieldType STORED_ONLY = new FieldType(false, true, false);
    public static final FieldType STRING_INDEXED_STORED = new FieldType(true, true, false);

    private final boolean indexed;
    private final boolean stored;
    private final boolean tokenized;

    public FieldType(boolean indexed, boolean stored, boolean tokenized) {
        this.indexed = indexed;
        this.stored = stored;
        this.tokenized = tokenized;
    }

    public boolean isIndexed() { return indexed; }
    public boolean isStored() { return stored; }
    public boolean isTokenized() { return tokenized; }
}

package com.github.lucene.simple.document;

import java.io.Serializable;

public class Field implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String value;
    private final FieldType fieldType;

    public Field(String name, String value, FieldType fieldType) {
        this.name = name;
        this.value = value;
        this.fieldType = fieldType;
    }

    public String name() { return name; }
    public String stringValue() { return value; }
    public FieldType fieldType() { return fieldType; }
}

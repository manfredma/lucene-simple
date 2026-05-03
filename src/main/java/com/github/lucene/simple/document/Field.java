package com.github.lucene.simple.document;

public class Field {
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

package com.github.lucene.simple.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Document {
    private final List<Field> fields = new ArrayList<>();

    public void add(Field field) {
        fields.add(field);
    }

    public Field getField(String name) {
        for (Field f : fields) {
            if (f.name().equals(name)) return f;
        }
        return null;
    }

    public List<Field> getFields(String name) {
        List<Field> result = new ArrayList<>();
        for (Field f : fields) {
            if (f.name().equals(name)) result.add(f);
        }
        return result;
    }

    public List<Field> getFields() {
        return Collections.unmodifiableList(fields);
    }
}

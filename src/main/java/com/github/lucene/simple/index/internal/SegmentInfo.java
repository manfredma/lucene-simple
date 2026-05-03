package com.github.lucene.simple.index.internal;

import com.github.lucene.simple.document.Document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SegmentInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Document> documents = new ArrayList<>();
    private final List<Boolean> deletedDocs = new ArrayList<>();

    public int addDocument(Document doc) {
        int docId = documents.size();
        documents.add(doc);
        deletedDocs.add(false);
        return docId;
    }

    public Document getDocument(int docId) {
        if (docId < 0 || docId >= documents.size() || deletedDocs.get(docId)) {
            return null;
        }
        return documents.get(docId);
    }

    public void deleteDocument(int docId) {
        if (docId >= 0 && docId < deletedDocs.size()) {
            deletedDocs.set(docId, true);
        }
    }

    public int numDocs() {
        int count = 0;
        for (Boolean deleted : deletedDocs) {
            if (!deleted) count++;
        }
        return count;
    }

    public int maxDoc() {
        return documents.size();
    }

    public List<Document> getAllDocuments() {
        return Collections.unmodifiableList(documents);
    }
}

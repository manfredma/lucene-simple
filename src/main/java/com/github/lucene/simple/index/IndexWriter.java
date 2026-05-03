package com.github.lucene.simple.index;

import com.github.lucene.simple.analysis.Analyzer;
import com.github.lucene.simple.analysis.Token;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.internal.SegmentInfo;
import com.github.lucene.simple.index.internal.TermDictionary;
import com.github.lucene.simple.store.Directory;

import java.util.List;

public class IndexWriter {
    private final Directory directory;
    private final Analyzer analyzer;
    private final TermDictionary termDictionary;
    private final SegmentInfo segmentInfo;
    private boolean closed = false;

    public IndexWriter(Directory directory, IndexWriterConfig config) {
        this.directory = directory;
        this.analyzer = config.getAnalyzer();
        this.termDictionary = directory.getTermDictionary() != null
                ? directory.getTermDictionary() : new TermDictionary();
        this.segmentInfo = directory.getSegmentInfo() != null
                ? directory.getSegmentInfo() : new SegmentInfo();
    }

    public void addDocument(Document doc) {
        checkNotClosed();
        int docId = segmentInfo.addDocument(doc);
        for (Field field : doc.getFields()) {
            FieldType ft = field.fieldType();
            if (!ft.isIndexed()) continue;
            if (ft.isTokenized()) {
                List<Token> tokens = analyzer.analyze(field.name(), field.stringValue());
                for (int pos = 0; pos < tokens.size(); pos++) {
                    Term term = new Term(field.name(), tokens.get(pos).getText());
                    termDictionary.addPosting(term, docId, pos);
                }
            } else {
                Term term = new Term(field.name(), field.stringValue());
                termDictionary.addPosting(term, docId, 0);
            }
        }
    }

    public void deleteDocuments(Term term) {
        checkNotClosed();
        com.github.lucene.simple.index.internal.PostingList pl = termDictionary.getPostingList(term);
        if (pl == null) return;
        for (int docId : pl.getDocIds()) {
            segmentInfo.deleteDocument(docId);
        }
    }

    public void commit() {
        checkNotClosed();
        directory.setTermDictionary(termDictionary);
        directory.setSegmentInfo(segmentInfo);
    }

    public void close() {
        commit();
        closed = true;
    }

    private void checkNotClosed() {
        if (closed) throw new IllegalStateException("IndexWriter is closed");
    }
}

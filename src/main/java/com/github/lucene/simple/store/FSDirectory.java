package com.github.lucene.simple.store;

import com.github.lucene.simple.index.internal.SegmentInfo;
import com.github.lucene.simple.index.internal.TermDictionary;

import java.io.*;
import java.nio.file.Path;

public class FSDirectory implements Directory {
    private final Path indexPath;
    private TermDictionary termDictionary;
    private SegmentInfo segmentInfo;

    private FSDirectory(Path indexPath) {
        this.indexPath = indexPath;
    }

    public static FSDirectory open(Path path) {
        return new FSDirectory(path);
    }

    public void save() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(indexPath.toFile())))) {
            oos.writeObject(termDictionary);
            oos.writeObject(segmentInfo);
        }
    }

    public void load() throws IOException, ClassNotFoundException {
        File file = indexPath.toFile();
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            termDictionary = (TermDictionary) ois.readObject();
            segmentInfo = (SegmentInfo) ois.readObject();
        }
    }

    @Override
    public TermDictionary getTermDictionary() { return termDictionary; }

    @Override
    public void setTermDictionary(TermDictionary termDictionary) { this.termDictionary = termDictionary; }

    @Override
    public SegmentInfo getSegmentInfo() { return segmentInfo; }

    @Override
    public void setSegmentInfo(SegmentInfo segmentInfo) { this.segmentInfo = segmentInfo; }

    @Override
    public void clear() {
        termDictionary = null;
        segmentInfo = null;
    }
}

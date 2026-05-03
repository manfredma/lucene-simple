package com.github.lucene.simple.analysis;

public final class Token {
    private final String text;
    private final int startOffset;
    private final int endOffset;

    public Token(String text, int startOffset, int endOffset) {
        this.text = text;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public String getText() { return text; }
    public int getStartOffset() { return startOffset; }
    public int getEndOffset() { return endOffset; }

    @Override
    public String toString() {
        return "Token{text='" + text + "', [" + startOffset + "," + endOffset + ")}";
    }
}

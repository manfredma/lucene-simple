# lucene-simple

A simplified full-text search engine built with Java 8, designed to mirror the core API of Apache Lucene while keeping the internals transparent and educational.

[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/build-Maven-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-26%20passing-brightgreen.svg)](#testing)

## Overview

`lucene-simple` implements the complete pipeline of a search engine from scratch:

```
Document → Analyzer → Inverted Index → Query → TF-IDF Scoring → TopDocs
```

It is not a production system. It is a learning tool where every layer — tokenization, index construction, query execution, and relevance scoring — is visible, readable, and testable.

**Key design principles:**

- API surface aligned with Apache Lucene (`IndexWriter`, `IndexSearcher`, `TermQuery`, etc.)
- Single-responsibility classes, no framework magic
- Both English and Chinese tokenization supported
- In-memory and file system storage backends

## Features

| Layer | What's implemented |
|---|---|
| **Analysis** | `StandardAnalyzer` (English, stop words), `ChineseAnalyzer` (IKAnalyzer + fallback) |
| **Document** | `Document`, `Field`, `FieldType` with 4 preset field types |
| **Index** | `IndexWriter`, inverted index with position tracking, `IndexReader` |
| **Query** | `TermQuery`, `BooleanQuery` (MUST/SHOULD/MUST_NOT), `PhraseQuery`, `RangeQuery` |
| **Scoring** | `TFIDFSimilarity`: `score = sqrt(tf) × (log((N+1)/(df+1)) + 1)` |
| **Storage** | `RAMDirectory` (in-memory), `FSDirectory` (Java serialization) |

## Quick Start

### Requirements

- Java 8+
- Maven 3.x

### Build

```bash
mvn clean package -DskipTests -Dsort.skip=true
```

### Run the demo

```bash
mvn exec:java -Dexec.mainClass="com.github.lucene.simple.demo.LuceneDemo" -Dsort.skip=true
```

Expected output:

```
=== TermQuery: title contains '搜索' ===
Total hits: 3
  docId=4, score=0.3863, title=搜索引擎原理
  docId=0, score=0.3863, title=Lucene搜索引擎入门
  docId=1, score=0.3863, title=Elasticsearch分布式搜索

=== BooleanQuery: MUST 'lucene' AND MUST '搜索' ===
...
```

### Basic usage

```java
// 1. Open a directory
Directory dir = new RAMDirectory();

// 2. Write documents
IndexWriterConfig config = new IndexWriterConfig(new ChineseAnalyzer());
IndexWriter writer = new IndexWriter(dir, config);

Document doc = new Document();
doc.add(new Field("id",      "1",         FieldType.STRING_INDEXED_STORED));
doc.add(new Field("title",   "Lucene入门",  FieldType.TEXT_INDEXED_STORED));
doc.add(new Field("content", "倒排索引与TF-IDF", FieldType.TEXT_INDEXED_STORED));
writer.addDocument(doc);
writer.commit();
writer.close();

// 3. Search
IndexReader reader = DirectoryReader.open(dir);
IndexSearcher searcher = new IndexSearcher(reader);

Query q = new TermQuery(new Term("title", "lucene"));
TopDocs results = searcher.search(q, 10);

for (ScoreDoc sd : results.scoreDocs) {
    Document hit = reader.document(sd.docId);
    System.out.printf("docId=%d score=%.4f title=%s%n",
        sd.docId, sd.score, hit.getField("title").stringValue());
}
```

## Architecture

```
lucene-simple/
├── analysis/
│   ├── Analyzer.java          # Interface: List<Token> analyze(field, text)
│   ├── Token.java             # Value object: text + offsets
│   ├── StandardAnalyzer.java  # English: split on whitespace/punctuation, lowercase, stop words
│   └── ChineseAnalyzer.java   # Chinese: IKAnalyzer smart segmentation, English fallback
│
├── document/
│   ├── FieldType.java         # 4 presets: TEXT_INDEXED_STORED, STRING_INDEXED_STORED, ...
│   ├── Field.java             # Value object: name + value + FieldType
│   └── Document.java          # Container: multiple fields per document
│
├── index/
│   ├── Term.java              # Value object: field + text
│   ├── IndexWriter.java       # addDocument / deleteDocuments / commit
│   ├── IndexReader.java       # Abstract: numDocs / document(id) / getPostingList(term)
│   ├── DirectoryReader.java   # Factory: DirectoryReader.open(Directory)
│   ├── IndexWriterConfig.java # Analyzer + Similarity
│   └── internal/
│       ├── PostingList.java   # Map<docId, List<position>> + termFreq + docFreq
│       ├── TermDictionary.java# Map<Term, PostingList>
│       └── SegmentInfo.java   # Document store + deletion bitmap
│
├── search/
│   ├── Query.java             # Interface: Set<Integer> execute(IndexReader)
│   ├── TermQuery.java         # Looks up PostingList directly
│   ├── BooleanQuery.java      # MUST=intersection, SHOULD=union, MUST_NOT=difference
│   ├── PhraseQuery.java       # Position-based consecutive match
│   ├── RangeQuery.java        # Lexicographic range over term dictionary
│   ├── IndexSearcher.java     # execute → score → sort → TopN
│   ├── ScoreDoc.java          # docId + score
│   └── TopDocs.java           # totalHits + ScoreDoc[]
│
├── scoring/
│   ├── Similarity.java        # Interface: float score(term, docId, reader)
│   └── TFIDFSimilarity.java   # tf=sqrt(freq), idf=log((N+1)/(df+1))+1
│
├── store/
│   ├── Directory.java         # Interface: get/set TermDictionary + SegmentInfo
│   ├── RAMDirectory.java      # In-memory, no persistence
│   └── FSDirectory.java       # Java serialization to a single file
│
└── demo/
    └── LuceneDemo.java        # End-to-end: index 5 docs, run 5 query types
```

## Scoring

Relevance scores follow the classic TF-IDF model:

```
score(doc, query) = Σ tf(t, doc) × idf(t)

tf(t, doc)  = sqrt(termFreq)
idf(t)      = log((N + 1) / (docFreq + 1)) + 1
```

Where `N` is the total number of documents and `docFreq` is the number of documents containing the term.

## Query Types

### TermQuery

```java
new TermQuery(new Term("title", "lucene"))
```

Matches documents where the specified field contains the exact term.

### BooleanQuery

```java
new BooleanQuery.Builder()
    .add(new TermQuery(new Term("title", "lucene")), BooleanClause.Occur.MUST)
    .add(new TermQuery(new Term("content", "index")), BooleanClause.Occur.SHOULD)
    .add(new TermQuery(new Term("tag", "deprecated")), BooleanClause.Occur.MUST_NOT)
    .build()
```

| Occur | Semantics |
|---|---|
| `MUST` | Document must match (logical AND) |
| `SHOULD` | Document may match (logical OR) |
| `MUST_NOT` | Document must not match (logical NOT) |

### PhraseQuery

```java
new PhraseQuery.Builder()
    .setField("content")
    .add(new Term("content", "search"))
    .add(new Term("content", "engine"))
    .build()
```

Matches documents where the terms appear consecutively in the given order.

### RangeQuery

```java
new RangeQuery("id", "10", "20")
```

Matches terms in lexicographic order between `lowerTerm` and `upperTerm` (inclusive).

## Testing

```bash
mvn clean test -Dsort.skip=true
```

26 tests, 0 failures:

| Test class | Coverage |
|---|---|
| `StandardAnalyzerTest` | Tokenization, stop words, lowercasing |
| `ChineseAnalyzerTest` | Chinese segmentation, mixed-language fallback |
| `DocumentTest` | Multi-field operations |
| `IndexWriterTest` | Add/delete/commit lifecycle |
| `TermDictionaryTest` | Posting list management |
| `TFIDFSimilarityTest` | tf, idf, score computation |
| `IndexSearcherTest` | All 5 query types with scoring |
| `FSDirectoryTest` | Serialization roundtrip |

## Differences from Apache Lucene

This project simplifies several things intentionally:

| Aspect | lucene-simple | Apache Lucene |
|---|---|---|
| Index segments | Single segment per commit | Multi-segment with merging |
| Persistence | Java serialization | Custom binary codec |
| Scoring | TF-IDF only | BM25 default, pluggable |
| Query parsing | Programmatic only | `QueryParser` with DSL |
| Concurrency | Single-threaded | Thread-safe, NRT |
| Numeric fields | Lexicographic range | Dedicated `NumericDocValues` |

## License

MIT License. See [LICENSE](LICENSE) for details.

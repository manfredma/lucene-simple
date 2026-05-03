# lucene-simple 设计规范

## 目标

实现一个简版 Lucene，核心概念和接口与真实 Lucene 对齐，内部实现适度简化。既用于学习/教学（代码清晰、职责单一），也提供可运行 Demo（完整展示索引→查询→评分流程）。

## 技术栈

- Java 8
- Maven
- IKAnalyzer（中文分词）
- JUnit 4（测试）

## 功能范围

- 多字段文档模型（Document + Field + FieldType）
- Analyzer 体系：StandardAnalyzer（英文）、ChineseAnalyzer（IKAnalyzer，中文+英文 fallback）
- 倒排索引（内存构建，可序列化到磁盘）
- 多种 Query：TermQuery、BooleanQuery（MUST/SHOULD/MUST_NOT）、PhraseQuery、RangeQuery
- TF-IDF 相关性评分
- 持久化：内存为主，提供 `save(path)` / `load(path)`

## 接口对齐说明

| 真实 Lucene | 简版做法 | 原因 |
|---|---|---|
| LeafReader + CompositeReader | 单一 DirectoryReader | 简化合并逻辑 |
| 多 Segment 文件格式 | 单一序列化结构 | 避免格式解析复杂度 |
| Weight / Scorer 两阶段 | IndexSearcher 内直接完成 | 减少间接层 |
| TokenFilter 链式管道 | Analyzer 直接返回 List<Token> | 减少流式复杂度 |
| DocValues / StoredFields 分离 | Field 统一存储 | 简化字段模型 |

## 包结构

```
com.github.lucene.simple/
├── analysis/
│   ├── Analyzer.java          (接口)
│   ├── TokenStream.java       (接口)
│   ├── Token.java             (值对象: text, startOffset, endOffset)
│   ├── StandardAnalyzer.java  (英文: 空格+标点分词 + lowercase + 停用词)
│   └── ChineseAnalyzer.java   (IKAnalyzer 中文 + 英文 fallback)
├── document/
│   ├── Document.java          (文档，包含多个 Field)
│   ├── Field.java             (字段: name, value, FieldType)
│   └── FieldType.java         (indexed, stored, tokenized 三个 boolean)
├── index/
│   ├── IndexWriter.java       (addDocument, deleteDocuments, commit, close)
│   ├── IndexReader.java       (抽象: numDocs, document, terms)
│   ├── DirectoryReader.java   (open(Directory) 工厂方法)
│   ├── IndexWriterConfig.java (analyzer, similarity)
│   ├── Term.java              (field + text)
│   └── internal/
│       ├── TermDictionary.java  (Map<Term, PostingList>)
│       ├── PostingList.java     (docId列表 + 位置信息)
│       └── SegmentInfo.java     (文档存储 + 统计信息)
├── search/
│   ├── IndexSearcher.java     (search(Query, int n) → TopDocs)
│   ├── Query.java             (接口)
│   ├── TermQuery.java
│   ├── BooleanQuery.java      (Builder 模式，add(Query, Occur))
│   ├── BooleanClause.java     (Query + Occur)
│   ├── PhraseQuery.java       (add(Term) 按顺序)
│   ├── RangeQuery.java        (field, lowerTerm, upperTerm)
│   ├── TopDocs.java           (totalHits, ScoreDoc[])
│   └── ScoreDoc.java          (docId, score, shardIndex)
├── scoring/
│   ├── Similarity.java        (接口: tf, idf, score)
│   └── TFIDFSimilarity.java   (tf=sqrt(freq), idf=log((N+1)/(df+1))+1)
├── store/
│   ├── Directory.java         (接口: save, load, clear)
│   ├── RAMDirectory.java      (内存实现)
│   └── FSDirectory.java       (文件系统 open(Path), 序列化到单文件)
└── demo/
    └── LuceneDemo.java        (完整流程演示)
```

## 数据流

```
Document
  └─→ IndexWriter.addDocument()
        └─→ Analyzer.analyze(field.value) → List<Token>
              └─→ TermDictionary.addPosting(term, docId, position)
                    └─→ PostingList{docIds, positions}

IndexSearcher.search(Query, n)
  └─→ Query.execute(IndexReader) → 候选 docId 集合
        └─→ TFIDFSimilarity.score(term, docId, reader) → float
              └─→ TopDocs{totalHits, ScoreDoc[n]}
```

## 评分公式

- `tf(freq) = sqrt(freq)`（词在文档中出现次数的平方根）
- `idf(N, df) = log((N+1)/(df+1)) + 1`（N=总文档数，df=含该词文档数）
- `score = tf * idf`（多 term 求和）

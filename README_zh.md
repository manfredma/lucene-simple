# lucene-simple

基于 Java 8 实现的简版全文搜索引擎，接口与 Apache Lucene 对齐，内部实现清晰透明，适合学习和教学。

[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/构建-Maven-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/许可证-MIT-green.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/测试-26%20通过-brightgreen.svg)](#测试)

## 概述

`lucene-simple` 从零实现了搜索引擎的完整流水线：

```
文档 → 分词 → 倒排索引 → 查询 → TF-IDF 评分 → TopDocs
```

这不是一个生产系统，而是一个学习工具——每一层的实现（分词、索引构建、查询执行、相关性评分）都清晰可读、可测试。

**设计原则：**

- API 与 Apache Lucene 保持对齐（`IndexWriter`、`IndexSearcher`、`TermQuery` 等）
- 单一职责，无框架魔法
- 同时支持中英文分词
- 内存和文件系统两种存储后端

## 功能特性

| 层次 | 已实现内容 |
|---|---|
| **分词** | `StandardAnalyzer`（英文，含停用词）、`ChineseAnalyzer`（IKAnalyzer + 英文回退） |
| **文档** | `Document`、`Field`、`FieldType`（4 种预设字段类型） |
| **索引** | `IndexWriter`、带位置信息的倒排索引、`IndexReader` |
| **查询** | `TermQuery`、`BooleanQuery`（MUST/SHOULD/MUST_NOT）、`PhraseQuery`、`RangeQuery` |
| **评分** | `TFIDFSimilarity`：`score = sqrt(tf) × (log((N+1)/(df+1)) + 1)` |
| **存储** | `RAMDirectory`（内存）、`FSDirectory`（Java 序列化） |

## 快速开始

### 环境要求

- Java 8+
- Maven 3.x

### 构建

```bash
mvn clean package -DskipTests -Dsort.skip=true
```

### 运行演示

```bash
mvn exec:java -Dexec.mainClass="com.github.lucene.simple.demo.LuceneDemo" -Dsort.skip=true
```

预期输出：

```
=== TermQuery: title 包含 '搜索' ===
总命中数: 3
  docId=4, score=0.3863, title=搜索引擎原理
  docId=0, score=0.3863, title=Lucene搜索引擎入门
  docId=1, score=0.3863, title=Elasticsearch分布式搜索

=== BooleanQuery: MUST 'lucene' AND MUST '搜索' ===
...
```

### 基本用法

```java
// 1. 打开存储目录
Directory dir = new RAMDirectory();

// 2. 写入文档
IndexWriterConfig config = new IndexWriterConfig(new ChineseAnalyzer());
IndexWriter writer = new IndexWriter(dir, config);

Document doc = new Document();
doc.add(new Field("id",      "1",              FieldType.STRING_INDEXED_STORED));
doc.add(new Field("title",   "Lucene入门",      FieldType.TEXT_INDEXED_STORED));
doc.add(new Field("content", "倒排索引与TF-IDF", FieldType.TEXT_INDEXED_STORED));
writer.addDocument(doc);
writer.commit();
writer.close();

// 3. 搜索
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

## 架构

```
lucene-simple/
├── analysis/
│   ├── Analyzer.java          # 接口：List<Token> analyze(field, text)
│   ├── Token.java             # 值对象：text + 偏移量
│   ├── StandardAnalyzer.java  # 英文：空格/标点分词、小写化、停用词过滤（44个）
│   └── ChineseAnalyzer.java   # 中文：IKAnalyzer 智能分词，非中文回退英文分词器
│
├── document/
│   ├── FieldType.java         # 4种预设：TEXT_INDEXED_STORED、STRING_INDEXED_STORED ...
│   ├── Field.java             # 值对象：name + value + FieldType
│   └── Document.java          # 容器：每个文档支持多字段（含同名字段）
│
├── index/
│   ├── Term.java              # 值对象：field + text（含 equals/hashCode）
│   ├── IndexWriter.java       # addDocument / deleteDocuments / commit
│   ├── IndexReader.java       # 抽象：numDocs / document(id) / getPostingList(term)
│   ├── DirectoryReader.java   # 工厂：DirectoryReader.open(Directory)
│   ├── IndexWriterConfig.java # 配置：Analyzer + Similarity
│   └── internal/
│       ├── PostingList.java   # Map<docId, List<position>> + termFreq + docFreq
│       ├── TermDictionary.java# Map<Term, PostingList>
│       └── SegmentInfo.java   # 文档存储 + 删除标记
│
├── search/
│   ├── Query.java             # 接口：Set<Integer> execute(IndexReader)
│   ├── TermQuery.java         # 直接查 PostingList
│   ├── BooleanQuery.java      # MUST=交集、SHOULD=并集、MUST_NOT=补集
│   ├── PhraseQuery.java       # 基于位置的连续词序匹配
│   ├── RangeQuery.java        # 词典字典序范围查询
│   ├── IndexSearcher.java     # execute → score → 排序 → TopN
│   ├── ScoreDoc.java          # 值对象：docId + score
│   └── TopDocs.java           # totalHits + ScoreDoc[]
│
├── scoring/
│   ├── Similarity.java        # 接口：float score(term, docId, reader)
│   └── TFIDFSimilarity.java   # tf=sqrt(freq)，idf=log((N+1)/(df+1))+1
│
├── store/
│   ├── Directory.java         # 接口：get/set TermDictionary + SegmentInfo
│   ├── RAMDirectory.java      # 内存存储，不持久化
│   └── FSDirectory.java       # Java 序列化到单个文件
│
└── demo/
    └── LuceneDemo.java        # 端到端演示：索引 5 篇文档，执行 5 种查询
```

## 评分算法

相关性评分采用经典 TF-IDF 模型：

```
score(doc, query) = Σ tf(t, doc) × idf(t)

tf(t, doc)  = sqrt(词频)
idf(t)      = log((总文档数 N + 1) / (含该词文档数 df + 1)) + 1
```

多个查询词的得分累加，得分越高排名越靠前。

## 查询类型

### TermQuery（词项查询）

```java
new TermQuery(new Term("title", "lucene"))
```

查找指定字段中包含精确词项的文档。

### BooleanQuery（布尔查询）

```java
new BooleanQuery.Builder()
    .add(new TermQuery(new Term("title", "lucene")),     BooleanClause.Occur.MUST)
    .add(new TermQuery(new Term("content", "索引")),      BooleanClause.Occur.SHOULD)
    .add(new TermQuery(new Term("tag",     "deprecated")), BooleanClause.Occur.MUST_NOT)
    .build()
```

| Occur | 语义 |
|---|---|
| `MUST` | 文档必须匹配（逻辑与） |
| `SHOULD` | 文档可以匹配（逻辑或） |
| `MUST_NOT` | 文档必须不匹配（逻辑非） |

### PhraseQuery（短语查询）

```java
new PhraseQuery.Builder()
    .setField("content")
    .add(new Term("content", "搜索"))
    .add(new Term("content", "引擎"))
    .build()
```

查找词项按指定顺序连续出现的文档（基于位置信息）。

### RangeQuery（范围查询）

```java
new RangeQuery("id", "10", "20")
```

按字典序匹配词典中 `lowerTerm` 到 `upperTerm`（含边界）之间的所有词项。

## 字段类型

| 预设类型 | indexed | stored | tokenized | 适用场景 |
|---|:---:|:---:|:---:|---|
| `TEXT_INDEXED_STORED` | ✓ | ✓ | ✓ | 全文搜索字段（title、content） |
| `STRING_INDEXED_STORED` | ✓ | ✓ | ✗ | 精确匹配字段（id、category） |
| `TEXT_INDEXED_NOT_STORED` | ✓ | ✗ | ✓ | 仅索引不需要返回的字段 |
| `STORED_ONLY` | ✗ | ✓ | ✗ | 仅用于展示的辅助字段 |

## 测试

```bash
mvn clean test -Dsort.skip=true
```

26 个测试，全部通过：

| 测试类 | 覆盖内容 |
|---|---|
| `StandardAnalyzerTest` | 分词准确性、停用词、小写化 |
| `ChineseAnalyzerTest` | 中文分词、中英混合、回退逻辑 |
| `DocumentTest` | 多字段操作 |
| `IndexWriterTest` | 添加/删除/commit 完整生命周期 |
| `TermDictionaryTest` | PostingList 管理 |
| `TFIDFSimilarityTest` | tf、idf、score 计算验证 |
| `IndexSearcherTest` | 5 种查询类型的执行与评分 |
| `FSDirectoryTest` | 文件系统序列化往返 |

## 与 Apache Lucene 的差异

本项目在以下方面做了有意的简化：

| 方面 | lucene-simple | Apache Lucene |
|---|---|---|
| 索引分段 | 每次 commit 单段 | 多段 + 自动合并 |
| 持久化格式 | Java 序列化 | 自定义二进制 Codec |
| 评分模型 | 仅 TF-IDF | 默认 BM25，可插拔 |
| 查询解析 | 纯代码构建 | `QueryParser` + DSL 语法 |
| 并发安全 | 单线程 | 线程安全，支持 NRT |
| 数值字段 | 字典序范围 | 专用 `NumericDocValues` |

## 许可证

MIT License，详见 [LICENSE](LICENSE)。

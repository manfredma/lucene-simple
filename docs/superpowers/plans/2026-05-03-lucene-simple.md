# lucene-simple Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现一个接口对齐真实 Lucene、内部适度简化的搜索引擎库，支持中英文分词、倒排索引、多种查询类型、TF-IDF 评分和内存/磁盘持久化。

**Architecture:** 分为六层：analysis（分词）→ document（文档模型）→ index（索引构建）→ search（查询执行）→ scoring（相关性评分）→ store（持久化）。各层通过接口解耦，IndexWriter 协调分词和索引，IndexSearcher 协调查询和评分。

**Tech Stack:** Java 8, Maven, IKAnalyzer 6.5.0（中文分词），JUnit 4.13.2

---

## 文件清单

| 文件 | 职责 |
|------|------|
| `pom.xml` | Maven 配置，引入 IKAnalyzer、JUnit 依赖 |
| `analysis/Token.java` | 词元值对象：text, startOffset, endOffset |
| `analysis/TokenStream.java` | 词元流接口 |
| `analysis/Analyzer.java` | 分析器接口：analyze(String) → List<Token> |
| `analysis/StandardAnalyzer.java` | 英文分析器：小写化+标点分隔+停用词 |
| `analysis/ChineseAnalyzer.java` | 中文分析器：IKAnalyzer分词 |
| `document/FieldType.java` | 字段类型：indexed/stored/tokenized |
| `document/Field.java` | 文档字段：name + value + FieldType |
| `document/Document.java` | 文档：字段列表，add/get/getFields |
| `index/Term.java` | 词项：field + text，实现 equals/hashCode |
| `index/internal/PostingList.java` | 倒排列表：docId列表+每个doc的位置列表 |
| `index/internal/TermDictionary.java` | 词典：Map<Term, PostingList> |
| `index/internal/SegmentInfo.java` | 段信息：存储文档原始字段、统计docCount |
| `index/IndexWriterConfig.java` | 写索引配置：analyzer, similarity |
| `index/IndexWriter.java` | 写索引：addDocument, deleteDocuments, commit, close |
| `index/IndexReader.java` | 读索引抽象类：numDocs, document, getPostingList, getDocFreq |
| `index/DirectoryReader.java` | 从 Directory 打开 IndexReader |
| `scoring/Similarity.java` | 评分接口：score(term, docId, reader) |
| `scoring/TFIDFSimilarity.java` | TF-IDF 实现 |
| `search/Query.java` | 查询接口：execute(IndexReader) → Set<Integer> |
| `search/ScoreDoc.java` | 单条结果：docId + score |
| `search/TopDocs.java` | 结果集：totalHits + ScoreDoc[] |
| `search/BooleanClause.java` | Boolean子句：Query + Occur(MUST/SHOULD/MUST_NOT) |
| `search/TermQuery.java` | 词项查询 |
| `search/BooleanQuery.java` | 布尔查询，Builder模式 |
| `search/PhraseQuery.java` | 短语查询（词序匹配） |
| `search/RangeQuery.java` | 范围查询（字典序比较） |
| `search/IndexSearcher.java` | 查询执行器：search(Query, n) → TopDocs |
| `store/Directory.java` | 存储接口：getTermDictionary, getSegmentInfo, save, load, clear |
| `store/RAMDirectory.java` | 内存实现 |
| `store/FSDirectory.java` | 文件系统实现（Java序列化到单文件） |
| `demo/LuceneDemo.java` | 完整流程演示（含中英文） |
| `test/...` | 每层对应单元测试 |

---

## Task 1: Maven 项目初始化

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.github.lucene</groupId>
    <artifactId>lucene-simple</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- IKAnalyzer 中文分词 -->
        <dependency>
            <groupId>com.janeluo</groupId>
            <artifactId>ikanalyzer</artifactId>
            <version>2012_u6</version>
        </dependency>

        <!-- JUnit 测试 -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建源码目录结构**

```bash
mkdir -p src/main/java/com/github/lucene/simple/{analysis,document,index/internal,search,scoring,store,demo}
mkdir -p src/test/java/com/github/lucene/simple/{analysis,document,index,search,scoring,store}
```

- [ ] **Step 3: 验证编译通过**

```bash
mvn clean compile -Dsort.skip=true
```

期望输出：`BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add pom.xml src/
git commit -m "chore: init maven project structure"
```

---

## Task 2: analysis 层 — Token、TokenStream、Analyzer

**Files:**
- Create: `src/main/java/com/github/lucene/simple/analysis/Token.java`
- Create: `src/main/java/com/github/lucene/simple/analysis/TokenStream.java`
- Create: `src/main/java/com/github/lucene/simple/analysis/Analyzer.java`
- Test: `src/test/java/com/github/lucene/simple/analysis/AnalyzerTest.java`（Task 3 的测试先写在这里）

- [ ] **Step 1: 写 Token.java**

```java
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
```

- [ ] **Step 2: 写 TokenStream.java**

```java
package com.github.lucene.simple.analysis;

import java.util.Iterator;

public interface TokenStream extends Iterator<Token> {
    boolean hasNext();
    Token next();
}
```

- [ ] **Step 3: 写 Analyzer.java**

```java
package com.github.lucene.simple.analysis;

import java.util.List;

public interface Analyzer {
    /**
     * 对输入文本进行分词，返回 Token 列表。
     * fieldName 参数预留，允许按字段定制分析策略。
     */
    List<Token> analyze(String fieldName, String text);
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/github/lucene/simple/analysis/
git commit -m "feat: add analysis layer interfaces (Token, TokenStream, Analyzer)"
```

---

## Task 3: StandardAnalyzer

**Files:**
- Create: `src/main/java/com/github/lucene/simple/analysis/StandardAnalyzer.java`
- Test: `src/test/java/com/github/lucene/simple/analysis/StandardAnalyzerTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.github.lucene.simple.analysis;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class StandardAnalyzerTest {

    private final Analyzer analyzer = new StandardAnalyzer();

    @Test
    public void testLowercaseAndSplit() {
        List<Token> tokens = analyzer.analyze("content", "Hello World");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
        assertEquals("world", tokens.get(1).getText());
    }

    @Test
    public void testPunctuationRemoved() {
        List<Token> tokens = analyzer.analyze("content", "hello, world!");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
        assertEquals("world", tokens.get(1).getText());
    }

    @Test
    public void testStopWords() {
        List<Token> tokens = analyzer.analyze("content", "the quick brown fox");
        // "the" 是停用词，应被过滤
        assertEquals(3, tokens.size());
        assertEquals("quick", tokens.get(0).getText());
    }

    @Test
    public void testEmptyInput() {
        List<Token> tokens = analyzer.analyze("content", "");
        assertTrue(tokens.isEmpty());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn clean test -pl . -Dtest=StandardAnalyzerTest -Dsort.skip=true
```

期望：FAIL，`StandardAnalyzer` 不存在。

- [ ] **Step 3: 实现 StandardAnalyzer.java**

```java
package com.github.lucene.simple.analysis;

import java.util.*;

public class StandardAnalyzer implements Analyzer {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "shall",
        "should", "may", "might", "must", "can", "could", "of", "in", "on",
        "at", "to", "for", "with", "by", "from", "and", "or", "but", "not"
    ));

    @Override
    public List<Token> analyze(String fieldName, String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        List<Token> tokens = new ArrayList<>();
        // 按非字母数字字符分割
        int i = 0;
        int len = text.length();
        while (i < len) {
            // 跳过非字母数字
            while (i < len && !Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
            if (i >= len) break;
            int start = i;
            while (i < len && Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
            String word = text.substring(start, i).toLowerCase();
            if (!STOP_WORDS.contains(word) && !word.isEmpty()) {
                tokens.add(new Token(word, start, i));
            }
        }
        return tokens;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn clean test -Dtest=StandardAnalyzerTest -Dsort.skip=true
```

期望：`Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: implement StandardAnalyzer with stop words and tokenization"
```

---

## Task 4: ChineseAnalyzer（IKAnalyzer）

**Files:**
- Create: `src/main/java/com/github/lucene/simple/analysis/ChineseAnalyzer.java`
- Test: `src/test/java/com/github/lucene/simple/analysis/ChineseAnalyzerTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.github.lucene.simple.analysis;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ChineseAnalyzerTest {

    private final Analyzer analyzer = new ChineseAnalyzer();

    @Test
    public void testChineseTokenization() {
        List<Token> tokens = analyzer.analyze("content", "中文分词测试");
        // IKAnalyzer 至少能切出多个词
        assertTrue("应切出至少1个词", tokens.size() >= 1);
        // 每个 token 不为空
        for (Token t : tokens) {
            assertFalse(t.getText().isEmpty());
        }
    }

    @Test
    public void testEnglishFallback() {
        List<Token> tokens = analyzer.analyze("content", "hello world");
        assertEquals(2, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
    }

    @Test
    public void testMixedContent() {
        List<Token> tokens = analyzer.analyze("content", "Lucene搜索引擎");
        assertTrue("混合内容应切出多个词", tokens.size() >= 2);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn clean test -Dtest=ChineseAnalyzerTest -Dsort.skip=true
```

期望：FAIL。

- [ ] **Step 3: 实现 ChineseAnalyzer.java**

```java
package com.github.lucene.simple.analysis;

import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class ChineseAnalyzer implements Analyzer {

    private final StandardAnalyzer fallback = new StandardAnalyzer();

    @Override
    public List<Token> analyze(String fieldName, String text) {
        if (text == null || text.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 判断是否包含中文字符
        if (!containsChinese(text)) {
            return fallback.analyze(fieldName, text);
        }
        List<Token> tokens = new ArrayList<>();
        try {
            IKSegmenter segmenter = new IKSegmenter(new StringReader(text), true);
            Lexeme lexeme;
            while ((lexeme = segmenter.next()) != null) {
                String word = lexeme.getLexemeText().toLowerCase().trim();
                if (!word.isEmpty()) {
                    tokens.add(new Token(word, lexeme.getBeginPosition(), lexeme.getEndPosition()));
                }
            }
        } catch (Exception e) {
            // IKAnalyzer 失败时降级到 StandardAnalyzer
            return fallback.analyze(fieldName, text);
        }
        return tokens;
    }

    private boolean containsChinese(String text) {
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn clean test -Dtest=ChineseAnalyzerTest -Dsort.skip=true
```

期望：`Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: implement ChineseAnalyzer using IKAnalyzer with English fallback"
```

---

## Task 5: document 层 — FieldType、Field、Document

**Files:**
- Create: `src/main/java/com/github/lucene/simple/document/FieldType.java`
- Create: `src/main/java/com/github/lucene/simple/document/Field.java`
- Create: `src/main/java/com/github/lucene/simple/document/Document.java`
- Test: `src/test/java/com/github/lucene/simple/document/DocumentTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.github.lucene.simple.document;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class DocumentTest {

    @Test
    public void testAddAndGetField() {
        Document doc = new Document();
        doc.add(new Field("title", "Hello World", FieldType.TEXT_INDEXED_STORED));
        Field f = doc.getField("title");
        assertNotNull(f);
        assertEquals("Hello World", f.stringValue());
    }

    @Test
    public void testMultipleFields() {
        Document doc = new Document();
        doc.add(new Field("title", "Lucene", FieldType.TEXT_INDEXED_STORED));
        doc.add(new Field("body", "search engine", FieldType.TEXT_INDEXED_STORED));
        List<Field> fields = doc.getFields();
        assertEquals(2, fields.size());
    }

    @Test
    public void testFieldType() {
        FieldType type = FieldType.TEXT_INDEXED_STORED;
        assertTrue(type.isIndexed());
        assertTrue(type.isStored());
        assertTrue(type.isTokenized());

        FieldType storedOnly = FieldType.STORED_ONLY;
        assertFalse(storedOnly.isIndexed());
        assertTrue(storedOnly.isStored());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn clean test -Dtest=DocumentTest -Dsort.skip=true
```

- [ ] **Step 3: 实现 FieldType.java**

```java
package com.github.lucene.simple.document;

public final class FieldType {

    public static final FieldType TEXT_INDEXED_STORED = new FieldType(true, true, true);
    public static final FieldType TEXT_INDEXED_NOT_STORED = new FieldType(true, false, true);
    public static final FieldType STORED_ONLY = new FieldType(false, true, false);
    public static final FieldType STRING_INDEXED_STORED = new FieldType(true, true, false);

    private final boolean indexed;
    private final boolean stored;
    private final boolean tokenized;

    public FieldType(boolean indexed, boolean stored, boolean tokenized) {
        this.indexed = indexed;
        this.stored = stored;
        this.tokenized = tokenized;
    }

    public boolean isIndexed() { return indexed; }
    public boolean isStored() { return stored; }
    public boolean isTokenized() { return tokenized; }
}
```

- [ ] **Step 4: 实现 Field.java**

```java
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
```

- [ ] **Step 5: 实现 Document.java**

```java
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
```

- [ ] **Step 6: 运行测试确认通过**

```bash
mvn clean test -Dtest=DocumentTest -Dsort.skip=true
```

期望：`Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 7: Commit**

```bash
git add src/
git commit -m "feat: implement document layer (FieldType, Field, Document)"
```

---

## Task 6: index 层内部结构 — Term、PostingList、TermDictionary、SegmentInfo

**Files:**
- Create: `src/main/java/com/github/lucene/simple/index/Term.java`
- Create: `src/main/java/com/github/lucene/simple/index/internal/PostingList.java`
- Create: `src/main/java/com/github/lucene/simple/index/internal/TermDictionary.java`
- Create: `src/main/java/com/github/lucene/simple/index/internal/SegmentInfo.java`
- Test: `src/test/java/com/github/lucene/simple/index/TermDictionaryTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.github.lucene.simple.index;

import com.github.lucene.simple.index.internal.PostingList;
import com.github.lucene.simple.index.internal.TermDictionary;
import org.junit.Test;
import static org.junit.Assert.*;

public class TermDictionaryTest {

    @Test
    public void testAddAndGetPosting() {
        TermDictionary dict = new TermDictionary();
        Term term = new Term("content", "lucene");
        dict.addPosting(term, 0, 0);
        dict.addPosting(term, 1, 0);

        PostingList pl = dict.getPostingList(term);
        assertNotNull(pl);
        assertEquals(2, pl.getDocIds().size());
        assertTrue(pl.getDocIds().contains(0));
        assertTrue(pl.getDocIds().contains(1));
    }

    @Test
    public void testDocFreq() {
        TermDictionary dict = new TermDictionary();
        Term term = new Term("content", "search");
        dict.addPosting(term, 0, 0);
        dict.addPosting(term, 0, 1); // 同一文档第二次出现
        dict.addPosting(term, 2, 0);

        assertEquals(2, dict.getDocFreq(term)); // 两个不同文档
    }

    @Test
    public void testTermEquals() {
        Term t1 = new Term("content", "hello");
        Term t2 = new Term("content", "hello");
        Term t3 = new Term("title", "hello");
        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn clean test -Dtest=TermDictionaryTest -Dsort.skip=true
```

- [ ] **Step 3: 实现 Term.java**

```java
package com.github.lucene.simple.index;

import java.io.Serializable;
import java.util.Objects;

public final class Term implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String field;
    private final String text;

    public Term(String field, String text) {
        this.field = field;
        this.text = text;
    }

    public String field() { return field; }
    public String text() { return text; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Term)) return false;
        Term term = (Term) o;
        return Objects.equals(field, term.field) && Objects.equals(text, term.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, text);
    }

    @Override
    public String toString() {
        return field + ":" + text;
    }
}
```

- [ ] **Step 4: 实现 PostingList.java**

```java
package com.github.lucene.simple.index.internal;

import java.io.Serializable;
import java.util.*;

public class PostingList implements Serializable {
    private static final long serialVersionUID = 1L;

    // docId -> 该词在文档中出现的位置列表
    private final Map<Integer, List<Integer>> postings = new LinkedHashMap<>();

    public void addPosting(int docId, int position) {
        postings.computeIfAbsent(docId, k -> new ArrayList<>()).add(position);
    }

    public Set<Integer> getDocIds() {
        return Collections.unmodifiableSet(postings.keySet());
    }

    public List<Integer> getPositions(int docId) {
        List<Integer> pos = postings.get(docId);
        return pos != null ? Collections.unmodifiableList(pos) : Collections.emptyList();
    }

    /** 词频：该词在指定文档中出现的次数 */
    public int getTermFreq(int docId) {
        List<Integer> pos = postings.get(docId);
        return pos != null ? pos.size() : 0;
    }

    /** 文档频率：包含该词的文档数量 */
    public int getDocFreq() {
        return postings.size();
    }
}
```

- [ ] **Step 5: 实现 TermDictionary.java**

```java
package com.github.lucene.simple.index.internal;

import com.github.lucene.simple.index.Term;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TermDictionary implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Term, PostingList> dictionary = new HashMap<>();

    public void addPosting(Term term, int docId, int position) {
        dictionary.computeIfAbsent(term, k -> new PostingList()).addPosting(docId, position);
    }

    public PostingList getPostingList(Term term) {
        return dictionary.get(term);
    }

    public int getDocFreq(Term term) {
        PostingList pl = dictionary.get(term);
        return pl != null ? pl.getDocFreq() : 0;
    }

    public Set<Term> terms() {
        return Collections.unmodifiableSet(dictionary.keySet());
    }
}
```

- [ ] **Step 6: 实现 SegmentInfo.java**

```java
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
```

- [ ] **Step 7: 运行测试确认通过**

```bash
mvn clean test -Dtest=TermDictionaryTest -Dsort.skip=true
```

期望：`Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 8: Commit**

```bash
git add src/
git commit -m "feat: implement index internal structures (Term, PostingList, TermDictionary, SegmentInfo)"
```

---

## Task 7: IndexWriterConfig、IndexWriter、IndexReader、DirectoryReader

**Files:**
- Create: `src/main/java/com/github/lucene/simple/index/IndexWriterConfig.java`
- Create: `src/main/java/com/github/lucene/simple/index/IndexWriter.java`
- Create: `src/main/java/com/github/lucene/simple/index/IndexReader.java`
- Create: `src/main/java/com/github/lucene/simple/index/DirectoryReader.java`
- Test: `src/test/java/com/github/lucene/simple/index/IndexWriterTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.github.lucene.simple.index;

import com.github.lucene.simple.analysis.StandardAnalyzer;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.internal.PostingList;
import com.github.lucene.simple.store.RAMDirectory;
import org.junit.Test;
import static org.junit.Assert.*;

public class IndexWriterTest {

    @Test
    public void testAddDocumentAndRead() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(dir, config);

        Document doc = new Document();
        doc.add(new Field("title", "Lucene search engine", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc);
        writer.commit();
        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        assertEquals(1, reader.numDocs());

        Document stored = reader.document(0);
        assertNotNull(stored);
        assertEquals("Lucene search engine", stored.getField("title").stringValue());
    }

    @Test
    public void testIndexContainsTerms() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));

        Document doc = new Document();
        doc.add(new Field("content", "hello world", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc);
        writer.commit();
        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        PostingList pl = reader.getPostingList(new Term("content", "hello"));
        assertNotNull(pl);
        assertTrue(pl.getDocIds().contains(0));
    }

    @Test
    public void testDeleteDocuments() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));

        Document doc1 = new Document();
        doc1.add(new Field("id", "1", FieldType.STRING_INDEXED_STORED));
        doc1.add(new Field("content", "hello", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc1);

        Document doc2 = new Document();
        doc2.add(new Field("id", "2", FieldType.STRING_INDEXED_STORED));
        doc2.add(new Field("content", "world", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc2);
        writer.commit();

        writer.deleteDocuments(new Term("id", "1"));
        writer.commit();
        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        assertEquals(1, reader.numDocs());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn clean test -Dtest=IndexWriterTest -Dsort.skip=true
```

- [ ] **Step 3: 实现 IndexWriterConfig.java**

```java
package com.github.lucene.simple.index;

import com.github.lucene.simple.analysis.Analyzer;
import com.github.lucene.simple.analysis.StandardAnalyzer;
import com.github.lucene.simple.scoring.Similarity;

public class IndexWriterConfig {
    private final Analyzer analyzer;
    private Similarity similarity;

    public IndexWriterConfig(Analyzer analyzer) {
        this.analyzer = analyzer;
        // similarity 默认 null，IndexSearcher 自带 TFIDFSimilarity
    }

    public Analyzer getAnalyzer() { return analyzer; }
    public Similarity getSimilarity() { return similarity; }
    public IndexWriterConfig setSimilarity(Similarity similarity) {
        this.similarity = similarity;
        return this;
    }
}
```

- [ ] **Step 4: 实现 IndexReader.java**

```java
package com.github.lucene.simple.index;

import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.index.internal.PostingList;

public abstract class IndexReader {
    public abstract int numDocs();
    public abstract int maxDoc();
    public abstract Document document(int docId);
    public abstract PostingList getPostingList(Term term);
    public abstract int getDocFreq(Term term);
}
```

- [ ] **Step 5: 实现 IndexWriter.java**

```java
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
        // 复用 directory 已有数据（支持追加写）
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
                // 不分词字段：整体作为一个 term
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
```

- [ ] **Step 6: 实现 DirectoryReader.java**

```java
package com.github.lucene.simple.index;

import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.index.internal.PostingList;
import com.github.lucene.simple.index.internal.SegmentInfo;
import com.github.lucene.simple.index.internal.TermDictionary;
import com.github.lucene.simple.store.Directory;

public class DirectoryReader extends IndexReader {
    private final TermDictionary termDictionary;
    private final SegmentInfo segmentInfo;

    private DirectoryReader(TermDictionary termDictionary, SegmentInfo segmentInfo) {
        this.termDictionary = termDictionary;
        this.segmentInfo = segmentInfo;
    }

    public static DirectoryReader open(Directory directory) {
        TermDictionary td = directory.getTermDictionary();
        SegmentInfo si = directory.getSegmentInfo();
        if (td == null) td = new TermDictionary();
        if (si == null) si = new SegmentInfo();
        return new DirectoryReader(td, si);
    }

    @Override
    public int numDocs() { return segmentInfo.numDocs(); }

    @Override
    public int maxDoc() { return segmentInfo.maxDoc(); }

    @Override
    public Document document(int docId) { return segmentInfo.getDocument(docId); }

    @Override
    public PostingList getPostingList(Term term) { return termDictionary.getPostingList(term); }

    @Override
    public int getDocFreq(Term term) { return termDictionary.getDocFreq(term); }
}
```

- [ ] **Step 7: 运行测试确认通过**

```bash
mvn clean test -Dtest=IndexWriterTest -Dsort.skip=true
```

期望：`Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 8: Commit**

```bash
git add src/
git commit -m "feat: implement IndexWriter, IndexReader, DirectoryReader, IndexWriterConfig"
```

---

## Task 8: store 层 — Directory、RAMDirectory、FSDirectory

**Files:**
- Create: `src/main/java/com/github/lucene/simple/store/Directory.java`
- Create: `src/main/java/com/github/lucene/simple/store/RAMDirectory.java`
- Create: `src/main/java/com/github/lucene/simple/store/FSDirectory.java`
- Test: `src/test/java/com/github/lucene/simple/store/FSDirectoryTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.github.lucene.simple.store;

import com.github.lucene.simple.analysis.StandardAnalyzer;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import static org.junit.Assert.*;

public class FSDirectoryTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testSaveAndLoad() throws Exception {
        File indexFile = new File(tmpFolder.getRoot(), "test.idx");

        // 写索引
        FSDirectory dir = FSDirectory.open(indexFile.toPath());
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));
        Document doc = new Document();
        doc.add(new Field("title", "save and load test", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc);
        writer.close();
        dir.save();

        // 重新加载
        FSDirectory dir2 = FSDirectory.open(indexFile.toPath());
        dir2.load();
        IndexReader reader = DirectoryReader.open(dir2);
        assertEquals(1, reader.numDocs());
        assertEquals("save and load test", reader.document(0).getField("title").stringValue());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn clean test -Dtest=FSDirectoryTest -Dsort.skip=true
```

- [ ] **Step 3: 实现 Directory.java**

```java
package com.github.lucene.simple.store;

import com.github.lucene.simple.index.internal.SegmentInfo;
import com.github.lucene.simple.index.internal.TermDictionary;

public interface Directory {
    TermDictionary getTermDictionary();
    void setTermDictionary(TermDictionary termDictionary);
    SegmentInfo getSegmentInfo();
    void setSegmentInfo(SegmentInfo segmentInfo);
    void clear();
}
```

- [ ] **Step 4: 实现 RAMDirectory.java**

```java
package com.github.lucene.simple.store;

import com.github.lucene.simple.index.internal.SegmentInfo;
import com.github.lucene.simple.index.internal.TermDictionary;

public class RAMDirectory implements Directory {
    private TermDictionary termDictionary;
    private SegmentInfo segmentInfo;

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
```

- [ ] **Step 5: 实现 FSDirectory.java**

```java
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
```

- [ ] **Step 6: 运行测试确认通过**

```bash
mvn clean test -Dtest=FSDirectoryTest -Dsort.skip=true
```

期望：`Tests run: 1, Failures: 0, Errors: 0`

- [ ] **Step 7: Commit**

```bash
git add src/
git commit -m "feat: implement store layer (Directory, RAMDirectory, FSDirectory)"
```

---

## Task 9: scoring 层 — Similarity、TFIDFSimilarity

**Files:**
- Create: `src/main/java/com/github/lucene/simple/scoring/Similarity.java`
- Create: `src/main/java/com/github/lucene/simple/scoring/TFIDFSimilarity.java`
- Test: `src/test/java/com/github/lucene/simple/scoring/TFIDFSimilarityTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.github.lucene.simple.scoring;

import com.github.lucene.simple.analysis.StandardAnalyzer;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.*;
import com.github.lucene.simple.store.RAMDirectory;
import org.junit.Test;
import static org.junit.Assert.*;

public class TFIDFSimilarityTest {

    @Test
    public void testScoreHigherForMoreFrequent() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));

        // doc0: lucene 出现1次
        Document doc0 = new Document();
        doc0.add(new Field("content", "lucene search", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc0);

        // doc1: lucene 出现3次
        Document doc1 = new Document();
        doc1.add(new Field("content", "lucene lucene lucene engine", FieldType.TEXT_INDEXED_STORED));
        writer.addDocument(doc1);

        writer.commit();
        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        TFIDFSimilarity similarity = new TFIDFSimilarity();
        Term term = new Term("content", "lucene");

        float score0 = similarity.score(term, 0, reader);
        float score1 = similarity.score(term, 1, reader);

        assertTrue("出现更多次的文档评分应更高: score0=" + score0 + ", score1=" + score1,
                score1 > score0);
    }

    @Test
    public void testTfFormula() {
        TFIDFSimilarity sim = new TFIDFSimilarity();
        assertEquals((float) Math.sqrt(4), sim.tf(4), 0.001f);
        assertEquals((float) Math.sqrt(1), sim.tf(1), 0.001f);
    }

    @Test
    public void testIdfFormula() {
        TFIDFSimilarity sim = new TFIDFSimilarity();
        // idf(N=10, df=2) = log((10+1)/(2+1)) + 1
        float expected = (float) (Math.log((10.0 + 1) / (2.0 + 1)) + 1);
        assertEquals(expected, sim.idf(10, 2), 0.001f);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn clean test -Dtest=TFIDFSimilarityTest -Dsort.skip=true
```

- [ ] **Step 3: 实现 Similarity.java**

```java
package com.github.lucene.simple.scoring;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;

public interface Similarity {
    float score(Term term, int docId, IndexReader reader);
    float tf(int freq);
    float idf(int numDocs, int docFreq);
}
```

- [ ] **Step 4: 实现 TFIDFSimilarity.java**

```java
package com.github.lucene.simple.scoring;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.index.internal.PostingList;

public class TFIDFSimilarity implements Similarity {

    @Override
    public float score(Term term, int docId, IndexReader reader) {
        PostingList pl = reader.getPostingList(term);
        if (pl == null) return 0f;
        int freq = pl.getTermFreq(docId);
        if (freq == 0) return 0f;
        int numDocs = reader.numDocs();
        int docFreq = reader.getDocFreq(term);
        return tf(freq) * idf(numDocs, docFreq);
    }

    @Override
    public float tf(int freq) {
        return (float) Math.sqrt(freq);
    }

    @Override
    public float idf(int numDocs, int docFreq) {
        return (float) (Math.log((numDocs + 1.0) / (docFreq + 1.0)) + 1.0);
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
mvn clean test -Dtest=TFIDFSimilarityTest -Dsort.skip=true
```

期望：`Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add src/
git commit -m "feat: implement scoring layer (Similarity, TFIDFSimilarity)"
```

---

## Task 10: search 层 — Query 体系和 IndexSearcher

**Files:**
- Create: `src/main/java/com/github/lucene/simple/search/Query.java`
- Create: `src/main/java/com/github/lucene/simple/search/ScoreDoc.java`
- Create: `src/main/java/com/github/lucene/simple/search/TopDocs.java`
- Create: `src/main/java/com/github/lucene/simple/search/BooleanClause.java`
- Create: `src/main/java/com/github/lucene/simple/search/TermQuery.java`
- Create: `src/main/java/com/github/lucene/simple/search/BooleanQuery.java`
- Create: `src/main/java/com/github/lucene/simple/search/PhraseQuery.java`
- Create: `src/main/java/com/github/lucene/simple/search/RangeQuery.java`
- Create: `src/main/java/com/github/lucene/simple/search/IndexSearcher.java`
- Test: `src/test/java/com/github/lucene/simple/search/IndexSearcherTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.github.lucene.simple.search;

import com.github.lucene.simple.analysis.StandardAnalyzer;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.*;
import com.github.lucene.simple.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class IndexSearcherTest {

    private IndexSearcher searcher;
    private IndexReader reader;

    @Before
    public void setUp() throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()));

        Document doc0 = new Document();
        doc0.add(new Field("title", "Lucene search engine", FieldType.TEXT_INDEXED_STORED));
        doc0.add(new Field("id", "0", FieldType.STRING_INDEXED_STORED));
        writer.addDocument(doc0);

        Document doc1 = new Document();
        doc1.add(new Field("title", "Elasticsearch search platform", FieldType.TEXT_INDEXED_STORED));
        doc1.add(new Field("id", "1", FieldType.STRING_INDEXED_STORED));
        writer.addDocument(doc1);

        Document doc2 = new Document();
        doc2.add(new Field("title", "Apache Solr full text search", FieldType.TEXT_INDEXED_STORED));
        doc2.add(new Field("id", "2", FieldType.STRING_INDEXED_STORED));
        writer.addDocument(doc2);

        writer.commit();
        writer.close();

        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
    }

    @Test
    public void testTermQuery() {
        TopDocs results = searcher.search(new TermQuery(new Term("title", "search")), 10);
        assertEquals(3, results.getTotalHits()); // 三个文档都含 search
    }

    @Test
    public void testBooleanQueryMust() {
        BooleanQuery query = new BooleanQuery.Builder()
                .add(new TermQuery(new Term("title", "search")), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term("title", "lucene")), BooleanClause.Occur.MUST)
                .build();
        TopDocs results = searcher.search(query, 10);
        assertEquals(1, results.getTotalHits()); // 只有 doc0 同时含两词
    }

    @Test
    public void testBooleanQueryMustNot() {
        BooleanQuery query = new BooleanQuery.Builder()
                .add(new TermQuery(new Term("title", "search")), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term("title", "lucene")), BooleanClause.Occur.MUST_NOT)
                .build();
        TopDocs results = searcher.search(query, 10);
        assertEquals(2, results.getTotalHits()); // doc1 和 doc2
    }

    @Test
    public void testPhraseQuery() {
        PhraseQuery query = new PhraseQuery.Builder()
                .setField("title")
                .add(new Term("title", "lucene"))
                .add(new Term("title", "search"))
                .build();
        TopDocs results = searcher.search(query, 10);
        assertEquals(1, results.getTotalHits()); // "lucene search" 相邻
    }

    @Test
    public void testTopNResults() {
        TopDocs results = searcher.search(new TermQuery(new Term("title", "search")), 2);
        assertEquals(3, results.getTotalHits()); // totalHits 是真实命中数
        assertEquals(2, results.getScoreDocs().length); // 只返回 top 2
    }

    @Test
    public void testResultsOrderedByScore() {
        // doc0 中 lucene 只出现1次，doc1 没有 lucene
        // 查 "lucene" 只有 doc0 命中
        TopDocs results = searcher.search(new TermQuery(new Term("title", "lucene")), 10);
        assertEquals(1, results.getTotalHits());
        assertEquals(0, results.getScoreDocs()[0].getDocId());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn clean test -Dtest=IndexSearcherTest -Dsort.skip=true
```

- [ ] **Step 3: 实现基础值对象**

**Query.java:**
```java
package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;
import java.util.Set;

public interface Query {
    Set<Integer> execute(IndexReader reader);
}
```

**ScoreDoc.java:**
```java
package com.github.lucene.simple.search;

public class ScoreDoc {
    private final int docId;
    private final float score;

    public ScoreDoc(int docId, float score) {
        this.docId = docId;
        this.score = score;
    }

    public int getDocId() { return docId; }
    public float getScore() { return score; }

    @Override
    public String toString() {
        return "ScoreDoc{docId=" + docId + ", score=" + score + "}";
    }
}
```

**TopDocs.java:**
```java
package com.github.lucene.simple.search;

public class TopDocs {
    private final int totalHits;
    private final ScoreDoc[] scoreDocs;

    public TopDocs(int totalHits, ScoreDoc[] scoreDocs) {
        this.totalHits = totalHits;
        this.scoreDocs = scoreDocs;
    }

    public int getTotalHits() { return totalHits; }
    public ScoreDoc[] getScoreDocs() { return scoreDocs; }
}
```

**BooleanClause.java:**
```java
package com.github.lucene.simple.search;

public class BooleanClause {
    public enum Occur { MUST, SHOULD, MUST_NOT }

    private final Query query;
    private final Occur occur;

    public BooleanClause(Query query, Occur occur) {
        this.query = query;
        this.occur = occur;
    }

    public Query getQuery() { return query; }
    public Occur getOccur() { return occur; }
}
```

- [ ] **Step 4: 实现查询类**

**TermQuery.java:**
```java
package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.index.internal.PostingList;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TermQuery implements Query {
    private final Term term;

    public TermQuery(Term term) {
        this.term = term;
    }

    public Term getTerm() { return term; }

    @Override
    public Set<Integer> execute(IndexReader reader) {
        PostingList pl = reader.getPostingList(term);
        if (pl == null) return Collections.emptySet();
        return new HashSet<>(pl.getDocIds());
    }
}
```

**BooleanQuery.java:**
```java
package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;

import java.util.*;

public class BooleanQuery implements Query {
    private final List<BooleanClause> clauses;

    private BooleanQuery(List<BooleanClause> clauses) {
        this.clauses = Collections.unmodifiableList(clauses);
    }

    public List<BooleanClause> clauses() { return clauses; }

    @Override
    public Set<Integer> execute(IndexReader reader) {
        Set<Integer> result = null;
        Set<Integer> mustNot = new HashSet<>();

        for (BooleanClause clause : clauses) {
            Set<Integer> docs = clause.getQuery().execute(reader);
            switch (clause.getOccur()) {
                case MUST:
                    if (result == null) result = new HashSet<>(docs);
                    else result.retainAll(docs);
                    break;
                case SHOULD:
                    if (result == null) result = new HashSet<>(docs);
                    else result.addAll(docs);
                    break;
                case MUST_NOT:
                    mustNot.addAll(docs);
                    break;
            }
        }
        if (result == null) result = Collections.emptySet();
        result.removeAll(mustNot);
        return result;
    }

    public static class Builder {
        private final List<BooleanClause> clauses = new ArrayList<>();

        public Builder add(Query query, BooleanClause.Occur occur) {
            clauses.add(new BooleanClause(query, occur));
            return this;
        }

        public BooleanQuery build() {
            return new BooleanQuery(new ArrayList<>(clauses));
        }
    }
}
```

**PhraseQuery.java:**
```java
package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.index.internal.PostingList;

import java.util.*;

public class PhraseQuery implements Query {
    private final String field;
    private final List<Term> terms;

    private PhraseQuery(String field, List<Term> terms) {
        this.field = field;
        this.terms = Collections.unmodifiableList(terms);
    }

    @Override
    public Set<Integer> execute(IndexReader reader) {
        if (terms.isEmpty()) return Collections.emptySet();

        // 先获取第一个 term 的候选文档
        PostingList firstPl = reader.getPostingList(terms.get(0));
        if (firstPl == null) return Collections.emptySet();
        Set<Integer> candidates = new HashSet<>(firstPl.getDocIds());

        Set<Integer> result = new HashSet<>();
        for (int docId : candidates) {
            if (matchesPhrase(docId, reader)) {
                result.add(docId);
            }
        }
        return result;
    }

    private boolean matchesPhrase(int docId, IndexReader reader) {
        // 获取第一个 term 在该文档中的位置
        PostingList firstPl = reader.getPostingList(terms.get(0));
        if (firstPl == null) return false;
        List<Integer> startPositions = firstPl.getPositions(docId);

        for (int startPos : startPositions) {
            boolean phraseMatch = true;
            for (int i = 1; i < terms.size(); i++) {
                PostingList pl = reader.getPostingList(terms.get(i));
                if (pl == null || !pl.getPositions(docId).contains(startPos + i)) {
                    phraseMatch = false;
                    break;
                }
            }
            if (phraseMatch) return true;
        }
        return false;
    }

    public static class Builder {
        private String field;
        private final List<Term> terms = new ArrayList<>();

        public Builder setField(String field) {
            this.field = field;
            return this;
        }

        public Builder add(Term term) {
            terms.add(term);
            return this;
        }

        public PhraseQuery build() {
            return new PhraseQuery(field, new ArrayList<>(terms));
        }
    }
}
```

**RangeQuery.java:**
```java
package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.index.internal.TermDictionary;

import java.util.HashSet;
import java.util.Set;

public class RangeQuery implements Query {
    private final String field;
    private final String lowerTerm;   // inclusive, null 表示无下界
    private final String upperTerm;   // inclusive, null 表示无上界

    public RangeQuery(String field, String lowerTerm, String upperTerm) {
        this.field = field;
        this.lowerTerm = lowerTerm;
        this.upperTerm = upperTerm;
    }

    @Override
    public Set<Integer> execute(IndexReader reader) {
        Set<Integer> result = new HashSet<>();
        // 需要遍历词典中该字段的所有 term，字典序比较
        if (!(reader instanceof com.github.lucene.simple.index.DirectoryReader)) {
            return result;
        }
        com.github.lucene.simple.index.DirectoryReader dr =
                (com.github.lucene.simple.index.DirectoryReader) reader;
        for (Term term : dr.getTermDictionary().terms()) {
            if (!term.field().equals(field)) continue;
            String text = term.text();
            if (lowerTerm != null && text.compareTo(lowerTerm) < 0) continue;
            if (upperTerm != null && text.compareTo(upperTerm) > 0) continue;
            com.github.lucene.simple.index.internal.PostingList pl = reader.getPostingList(term);
            if (pl != null) result.addAll(pl.getDocIds());
        }
        return result;
    }
}
```

- [ ] **Step 5: DirectoryReader 暴露 getTermDictionary()**

在 `DirectoryReader.java` 中添加：

```java
public TermDictionary getTermDictionary() {
    return termDictionary;
}
```

- [ ] **Step 6: 实现 IndexSearcher.java**

```java
package com.github.lucene.simple.search;

import com.github.lucene.simple.index.IndexReader;
import com.github.lucene.simple.index.Term;
import com.github.lucene.simple.scoring.Similarity;
import com.github.lucene.simple.scoring.TFIDFSimilarity;

import java.util.*;

public class IndexSearcher {
    private final IndexReader reader;
    private final Similarity similarity;

    public IndexSearcher(IndexReader reader) {
        this.reader = reader;
        this.similarity = new TFIDFSimilarity();
    }

    public IndexSearcher(IndexReader reader, Similarity similarity) {
        this.reader = reader;
        this.similarity = similarity;
    }

    public TopDocs search(Query query, int n) {
        Set<Integer> matchingDocIds = query.execute(reader);
        int totalHits = matchingDocIds.size();

        List<ScoreDoc> scoreDocs = new ArrayList<>();
        for (int docId : matchingDocIds) {
            float score = computeScore(query, docId);
            scoreDocs.add(new ScoreDoc(docId, score));
        }

        scoreDocs.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        int size = Math.min(n, scoreDocs.size());
        ScoreDoc[] topN = scoreDocs.subList(0, size).toArray(new ScoreDoc[0]);
        return new TopDocs(totalHits, topN);
    }

    private float computeScore(Query query, int docId) {
        if (query instanceof TermQuery) {
            return similarity.score(((TermQuery) query).getTerm(), docId, reader);
        }
        if (query instanceof BooleanQuery) {
            float total = 0f;
            for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
                if (clause.getOccur() != BooleanClause.Occur.MUST_NOT) {
                    total += computeScore(clause.getQuery(), docId);
                }
            }
            return total;
        }
        if (query instanceof PhraseQuery) {
            // PhraseQuery 评分：对每个词的 TF-IDF 求平均
            PhraseQuery pq = (PhraseQuery) query;
            float total = 0f;
            int count = 0;
            for (Term term : extractTerms(pq)) {
                total += similarity.score(term, docId, reader);
                count++;
            }
            return count > 0 ? total / count : 0f;
        }
        // RangeQuery 等无法细粒度评分，返回1.0（命中即相关）
        return 1.0f;
    }

    private List<Term> extractTerms(PhraseQuery pq) {
        // 通过反射或包可见性获取 terms；这里用包访问
        return pq.getTerms();
    }
}
```

- [ ] **Step 7: PhraseQuery 暴露 getTerms()**

在 `PhraseQuery.java` 中添加：

```java
public List<Term> getTerms() {
    return terms;
}
```

- [ ] **Step 8: 运行测试确认通过**

```bash
mvn clean test -Dtest=IndexSearcherTest -Dsort.skip=true
```

期望：`Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 9: Commit**

```bash
git add src/
git commit -m "feat: implement search layer (Query, TermQuery, BooleanQuery, PhraseQuery, RangeQuery, IndexSearcher)"
```

---

## Task 11: Demo — LuceneDemo

**Files:**
- Create: `src/main/java/com/github/lucene/simple/demo/LuceneDemo.java`

- [ ] **Step 1: 实现 LuceneDemo.java**

```java
package com.github.lucene.simple.demo;

import com.github.lucene.simple.analysis.ChineseAnalyzer;
import com.github.lucene.simple.document.Document;
import com.github.lucene.simple.document.Field;
import com.github.lucene.simple.document.FieldType;
import com.github.lucene.simple.index.*;
import com.github.lucene.simple.search.*;
import com.github.lucene.simple.store.RAMDirectory;

public class LuceneDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== lucene-simple Demo ===\n");

        RAMDirectory dir = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new ChineseAnalyzer());
        IndexWriter writer = new IndexWriter(dir, config);

        // 添加文档
        String[][] data = {
            {"0", "Lucene搜索引擎入门", "Lucene是Apache开源的全文搜索引擎库，功能强大"},
            {"1", "Elasticsearch分布式搜索", "Elasticsearch基于Lucene实现，支持分布式搜索"},
            {"2", "Solr全文搜索平台", "Solr是Apache Lucene项目的子项目，企业级搜索平台"},
            {"3", "Java编程语言", "Java是一种面向对象的编程语言，广泛应用于企业开发"},
            {"4", "搜索引擎原理", "搜索引擎利用倒排索引实现高效的全文检索功能"},
        };

        for (String[] row : data) {
            Document doc = new Document();
            doc.add(new Field("id", row[0], FieldType.STRING_INDEXED_STORED));
            doc.add(new Field("title", row[1], FieldType.TEXT_INDEXED_STORED));
            doc.add(new Field("content", row[2], FieldType.TEXT_INDEXED_STORED));
            writer.addDocument(doc);
        }
        writer.commit();
        writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        System.out.println("总文档数: " + reader.numDocs() + "\n");

        // 1. TermQuery
        System.out.println("--- TermQuery: title 含 '搜索' ---");
        TopDocs results = searcher.search(new TermQuery(new Term("title", "搜索")), 10);
        printResults(results, reader);

        // 2. BooleanQuery MUST + MUST
        System.out.println("\n--- BooleanQuery: title 含 'lucene' AND content 含 '搜索' ---");
        BooleanQuery bq = new BooleanQuery.Builder()
                .add(new TermQuery(new Term("title", "lucene")), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term("content", "搜索")), BooleanClause.Occur.MUST)
                .build();
        printResults(searcher.search(bq, 10), reader);

        // 3. BooleanQuery MUST_NOT
        System.out.println("\n--- BooleanQuery: content 含 '搜索' BUT NOT '分布式' ---");
        BooleanQuery bq2 = new BooleanQuery.Builder()
                .add(new TermQuery(new Term("content", "搜索")), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term("content", "分布式")), BooleanClause.Occur.MUST_NOT)
                .build();
        printResults(searcher.search(bq2, 10), reader);

        // 4. PhraseQuery
        System.out.println("\n--- PhraseQuery: title 短语 'lucene 搜索' ---");
        PhraseQuery pq = new PhraseQuery.Builder()
                .setField("title")
                .add(new Term("title", "lucene"))
                .add(new Term("title", "搜索"))
                .build();
        printResults(searcher.search(pq, 10), reader);

        // 5. RangeQuery
        System.out.println("\n--- RangeQuery: id 在 [1, 3] 范围 ---");
        RangeQuery rq = new RangeQuery("id", "1", "3");
        printResults(searcher.search(rq, 10), reader);
    }

    private static void printResults(TopDocs topDocs, IndexReader reader) {
        System.out.println("命中: " + topDocs.getTotalHits() + " 条");
        for (ScoreDoc sd : topDocs.getScoreDocs()) {
            Document doc = reader.document(sd.getDocId());
            System.out.printf("  docId=%-2d score=%.4f  title=%s%n",
                    sd.getDocId(), sd.getScore(),
                    doc != null ? doc.getField("title").stringValue() : "N/A");
        }
    }
}
```

- [ ] **Step 2: 运行 Demo**

```bash
mvn clean compile -Dsort.skip=true && mvn exec:java -Dexec.mainClass="com.github.lucene.simple.demo.LuceneDemo" -Dsort.skip=true
```

期望：控制台打印 5 组查询结果，无异常。

- [ ] **Step 3: Commit**

```bash
git add src/
git commit -m "feat: add LuceneDemo showing full index/search/score pipeline"
```

---

## Task 12: 全量测试通过

- [ ] **Step 1: 运行所有测试**

```bash
mvn clean test -Dsort.skip=true
```

期望：所有测试通过，`BUILD SUCCESS`，失败数为0。

- [ ] **Step 2: Commit（如有修复）**

```bash
git add -A
git commit -m "test: all tests passing"
```

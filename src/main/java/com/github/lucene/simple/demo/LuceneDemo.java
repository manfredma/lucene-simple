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

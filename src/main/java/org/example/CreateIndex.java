package org.example;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public class CreateIndex {

    private static final String INDEX_DIRECTORY = "index";  // 索引存储目录

    public static void main(String[] args) throws IOException {
        EnglishAnalyzer analyzer = new EnglishAnalyzer();
        FSDirectory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);  // 创建新索引
        IndexWriter iwriter = new IndexWriter(directory, config);

        // 读取Cranfield Collection文档并索引
        BufferedReader reader = new BufferedReader(new FileReader("data/cran.all.1400"));
        String line;
        Document doc = null;
        StringBuilder content = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(".I")) {
                if (doc != null) {
                    doc.add(new TextField("content", content.toString(), Field.Store.YES));
                    iwriter.addDocument(doc);
                }
                doc = new Document();
                doc.add(new StringField("filename", line.substring(3).trim(), Field.Store.YES));
                content = new StringBuilder();
            } else if (line.startsWith(".W")) {
                content = new StringBuilder();  // 重置内容部分
            } else {
                content.append(line).append(" ");
            }
        }
        if (doc != null) {
            doc.add(new TextField("content", content.toString(), Field.Store.YES));
            iwriter.addDocument(doc);
        }
        reader.close();
        iwriter.close();
        directory.close();
    }
}

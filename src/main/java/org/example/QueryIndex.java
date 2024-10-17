package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class QueryIndex {

    private static final String INDEX_DIRECTORY = "index";  // 索引目录
    private static final String RESULTS_FILE = "results/query_results.txt";  // 查询结果输出文件
    private static final String QUERY_FILE = "data/cran.qry";  // 查询文件路径
    private static final int MAX_RESULTS = 50;  // 查询返回的最大结果数

    public static void main(String[] args) throws Exception {
        FSDirectory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        StandardAnalyzer analyzer = new StandardAnalyzer();

        // 设置评分模型 (BM25 或 Classic Similarity)
        isearcher.setSimilarity(new BM25Similarity());  // 或者使用 ClassicSimilarity() 为向量空间模型

        List<String> queries = parseQueryFile(QUERY_FILE);

        BufferedWriter writer = new BufferedWriter(new FileWriter(RESULTS_FILE));
        QueryParser parser = new QueryParser("content", analyzer);

        for (int i = 0; i < queries.size(); i++) {
            Query query = parser.parse(queries.get(i).trim());
            TopDocs results = isearcher.search(query, MAX_RESULTS);

            for (ScoreDoc hit : results.scoreDocs) {
                Document hitDoc = isearcher.doc(hit.doc);
                writer.write("Query ID: " + (i + 1) + ", Doc ID: " + hitDoc.get("filename") + ", Score: " + hit.score + "\n");
            }
        }
        writer.close();
        ireader.close();
    }

    private static List<String> parseQueryFile(String queryFilePath) throws Exception {
        List<String> queries = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(queryFilePath));
        String line;
        StringBuilder query = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(".I")) {
                if (query.length() > 0) {
                    queries.add(query.toString().trim());
                    query.setLength(0);
                }
            } else if (line.startsWith(".W")) {
                continue;
            } else {
                query.append(line).append(" ");
            }
        }
        if (query.length() > 0) {
            queries.add(query.toString().trim());
        }
        reader.close();
        return queries;
    }
}

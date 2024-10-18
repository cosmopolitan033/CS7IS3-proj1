package org.example;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Document;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
        EnglishAnalyzer analyzer = new EnglishAnalyzer();

        // 切换评分模型 (BM25 或 ClassicSimilarity)
        boolean useBM25 = true;  // 使用BM25
        if (useBM25) {
            isearcher.setSimilarity(new BM25Similarity(3.3f, 0.8f));
        } else {
            isearcher.setSimilarity(new ClassicSimilarity());
        }

        // 确保 results 目录存在
        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();  // 如果 results 目录不存在，创建它
        }

        List<String> queries = parseQueryFile(QUERY_FILE);

        BufferedWriter writer = new BufferedWriter(new FileWriter(RESULTS_FILE));
        QueryParser parser = new QueryParser("content", analyzer);
        parser.setAllowLeadingWildcard(false);  // 禁用前导通配符

        for (int i = 0; i < queries.size(); i++) {
            try {
                String processedQuery = queries.get(i).replaceAll("[*?]", "");  // 处理查询中的通配符
                Query query = parser.parse(processedQuery.trim());
                TopDocs results = isearcher.search(query, MAX_RESULTS);

                int rank = 1;  // 初始化排名
                for (ScoreDoc hit : results.scoreDocs) {
                    Document hitDoc = isearcher.doc(hit.doc);
                    // 将结果转换为TREC Eval格式
                    writer.write((i + 1) + " Q0 " + hitDoc.get("filename") + " " + rank++ + " " + hit.score + " STANDARD\n");
                }
            } catch (Exception e) {
                System.err.println("Error parsing query " + (i + 1) + ": " + e.getMessage());
            }
        }
        writer.close();
        ireader.close();
    }

    // 解析查询文件 cran.qry
    private static List<String> parseQueryFile(String queryFilePath) throws Exception {
        List<String> queries = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(queryFilePath));
        String line;
        StringBuilder query = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(".I")) {
                if (query.length() > 0) {
                    queries.add(query.toString().trim());
                    query.setLength(0);  // 清空之前的查询内容
                }
            } else if (line.startsWith(".W")) {
                continue;  // 忽略 .W 标记
            } else {
                query.append(line).append(" ");  // 累积查询内容
            }
        }
        if (query.length() > 0) {
            queries.add(query.toString().trim());  // 处理最后一个查询
        }
        reader.close();
        return queries;
    }
}

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

    private static final String INDEX_DIRECTORY = "index";
    private static final String QUERY_FILE = "data/cran.qry";
    private static final int MAX_RESULTS = 50;

    public static void main(String[] args) throws Exception {
        FSDirectory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        EnglishAnalyzer analyzer = new EnglishAnalyzer();

        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }

        List<String> queries = parseQueryFile(QUERY_FILE);

        runWithBM25(isearcher, analyzer, queries, 3.3f, 0.8f);
        runWithClassicSimilarity(isearcher, analyzer, queries);
        runWithLMDirichlet(isearcher, analyzer, queries, 1500);
        runWithLMDirichlet(isearcher, analyzer, queries, 2000);

        ireader.close();
    }

    private static void runWithBM25(IndexSearcher isearcher, EnglishAnalyzer analyzer, List<String> queries, float k1, float b) throws Exception {
        isearcher.setSimilarity(new BM25Similarity(k1, b));
        String resultFilename = "results/query_results_bm25_" + k1 + "_" + b + ".txt";
        searchAndWriteResults(isearcher, analyzer, queries, resultFilename);
    }

    private static void runWithClassicSimilarity(IndexSearcher isearcher, EnglishAnalyzer analyzer, List<String> queries) throws Exception {
        isearcher.setSimilarity(new ClassicSimilarity());
        String resultFilename = "results/query_results_classic.txt";
        searchAndWriteResults(isearcher, analyzer, queries, resultFilename);
    }

    private static void runWithLMDirichlet(IndexSearcher isearcher, EnglishAnalyzer analyzer, List<String> queries, float mu) throws Exception {
        isearcher.setSimilarity(new LMDirichletSimilarity(mu));
        String resultFilename = "results/query_results_lmdirichlet_" + mu + ".txt";
        searchAndWriteResults(isearcher, analyzer, queries, resultFilename);
    }

    private static void searchAndWriteResults(IndexSearcher isearcher, EnglishAnalyzer analyzer, List<String> queries, String resultFilename) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(resultFilename));
        QueryParser parser = new QueryParser("content", analyzer);
        parser.setAllowLeadingWildcard(false);

        for (int i = 0; i < queries.size(); i++) {
            try {
                String processedQuery = queries.get(i).replaceAll("[*?]", "");
                Query query = parser.parse(processedQuery.trim());
                TopDocs results = isearcher.search(query, MAX_RESULTS);

                int rank = 1;
                for (ScoreDoc hit : results.scoreDocs) {
                    Document hitDoc = isearcher.doc(hit.doc);
                    writer.write((i + 1) + " Q0 " + hitDoc.get("filename") + " " + rank++ + " " + hit.score + " STANDARD\n");
                }
            } catch (Exception e) {
                System.err.println("Error parsing query " + (i + 1) + ": " + e.getMessage());
            }
        }
        writer.close();
        System.out.println("Results written to: " + resultFilename);
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

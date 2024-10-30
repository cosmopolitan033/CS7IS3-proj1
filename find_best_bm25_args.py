import os
import subprocess
import re
import numpy as np

k1_values = np.arange(0.5, 3.5, 0.1)
b_values = np.arange(0.0, 1.1, 0.05)

def update_bm25_params(k1, b):
    with open("src/main/java/org/example/QueryIndex.java", "r") as file:
        lines = file.readlines()

    with open("src/main/java/org/example/QueryIndex.java", "w") as file:
        for line in lines:
            if "runWithBM25(isearcher, analyzer, queries," in line:
                file.write(f'        runWithBM25(isearcher, analyzer, queries, {k1}f, {b}f);\n')
            else:
                file.write(line)

def run_query_index():
    subprocess.run(["mvn", "clean", "package"], stdout=subprocess.PIPE)
    subprocess.run(["java", "-jar", "target/query-index.jar"], stdout=subprocess.PIPE)

def run_trec_eval(k1, b):
    filename = f"./results/query_results_bm25_{k1:.1f}_{b:.1f}.txt"
    result = subprocess.run(["./trec_eval-9.0.7/trec_eval", "./data/cranqrel", filename], stdout=subprocess.PIPE)
    output = result.stdout.decode("utf-8")
    match = re.search(r"map\s+all\s+([0-9.]+)", output)
    return float(match.group(1)) if match else 0

best_map = 0
best_k1 = 0
best_b = 0

# Ensure the results directory exists
os.makedirs("results", exist_ok=True)

for k1 in k1_values:
    for b in b_values:
        print(f"Testing BM25 with k1={k1:.2f} and b={b:.2f}")

        update_bm25_params(k1, b)
        run_query_index()

        map_score = run_trec_eval(k1, b)
        print(f"MAP score for k1={k1:.2f}, b={b:.2f}: {map_score}\n")

        if map_score > best_map:
            best_map = map_score
            best_k1 = k1
            best_b = b

print(f"Best BM25 parameters: k1={best_k1:.2f}, b={best_b:.2f} with MAP={best_map}")

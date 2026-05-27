#!/bin/bash

# Benchmark suite for CARBSS scheduler (Task 6)
# Runs all required benchmark instances and generates results

set -e

echo "=== CARBSS Benchmark Suite ==="
echo ""

# Create directories
mkdir -p instances
mkdir -p results

# Small instances (compare against brute-force optimal)
echo "Generating small instances..."
python3 generate_instance.py --n 8 --K 3 --density 0.3 --seed 1 --output instances/small_n8_K3.json
python3 generate_instance.py --n 10 --K 4 --density 0.4 --seed 2 --output instances/small_n10_K4.json
python3 generate_instance.py --n 12 --K 4 --density 0.5 --seed 3 --output instances/small_n12_K4.json

# Medium instances
echo "Generating medium instances..."
python3 generate_instance.py --n 50 --K 8 --density 0.25 --seed 10 --output instances/medium_n50_K8.json
python3 generate_instance.py --n 100 --K 10 --density 0.30 --seed 11 --output instances/medium_n100_K10.json
python3 generate_instance.py --n 150 --K 12 --density 0.35 --seed 12 --output instances/medium_n150_K12.json

# Stress instances
echo "Generating stress instances..."
python3 generate_instance.py --n 200 --K 15 --density 0.40 --seed 20 --output instances/stress_n200_K15.json
python3 generate_instance.py --n 200 --K 5 --density 0.60 --seed 21 --output instances/stress_n200_K5_tight.json
python3 generate_instance.py --n 200 --K 20 --density 0.10 --seed 22 --output instances/stress_n200_K20_sparse.json

echo ""
echo "Building CARBSS..."
mvn clean package -DskipTests

echo ""
echo "Running benchmarks..."
echo ""

# Run all instances
for instance in instances/*.json; do
    basename=$(basename "$instance" .json)
    echo "Running: $basename"
    java -jar target/credit-pipeline-scheduler-1.0.0.jar "$instance" "results/${basename}_result.json"
    echo ""
done

echo "=== Benchmark Complete ==="
echo "Results saved in results/ directory"

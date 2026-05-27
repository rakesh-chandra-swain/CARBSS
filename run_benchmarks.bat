@echo off
REM Benchmark suite for CARBSS scheduler (Task 6)
REM Windows batch file version

echo === CARBSS Benchmark Suite ===
echo.

REM Create directories
if not exist instances mkdir instances
if not exist results mkdir results

REM Small instances
echo Generating small instances...
python generate_instance.py --n 8 --K 3 --density 0.3 --seed 1 --output instances/small_n8_K3.json
python generate_instance.py --n 10 --K 4 --density 0.4 --seed 2 --output instances/small_n10_K4.json
python generate_instance.py --n 12 --K 4 --density 0.5 --seed 3 --output instances/small_n12_K4.json

REM Medium instances
echo Generating medium instances...
python generate_instance.py --n 50 --K 8 --density 0.25 --seed 10 --output instances/medium_n50_K8.json
python generate_instance.py --n 100 --K 10 --density 0.30 --seed 11 --output instances/medium_n100_K10.json
python generate_instance.py --n 150 --K 12 --density 0.35 --seed 12 --output instances/medium_n150_K12.json

REM Stress instances
echo Generating stress instances...
python generate_instance.py --n 200 --K 15 --density 0.40 --seed 20 --output instances/stress_n200_K15.json
python generate_instance.py --n 200 --K 5 --density 0.60 --seed 21 --output instances/stress_n200_K5_tight.json
python generate_instance.py --n 200 --K 20 --density 0.10 --seed 22 --output instances/stress_n200_K20_sparse.json

echo.
echo Building CARBSS...
call mvn clean package -DskipTests

echo.
echo Running benchmarks...
echo.

REM Run all instances
for %%f in (instances\*.json) do (
    echo Running: %%~nf
    java -jar target\credit-pipeline-scheduler-1.0.0.jar "%%f" "results\%%~nf_result.json"
    echo.
)

echo === Benchmark Complete ===
echo Results saved in results\ directory
pause

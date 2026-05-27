# How to Test CARBSS - Simple Guide

## Prerequisites
- Java 17+ installed
- Maven 3.8+ installed
- Python 3.10+ installed

---

## Method 1: Run All Unit Tests (Fastest)

```bash
cd CARBSS
mvn test
```

**What this does:**
- Runs all 8 unit tests
- Takes ~10 seconds
- Shows PASS/FAIL for each test

**Expected Output:**
```
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Method 2: Run Single Test

```bash
cd CARBSS
mvn test -Dtest=SchedulerTest#testToyInstance
```

**Available tests:**
- `testAllConflictGraph` - Tests infeasible case
- `testZeroCapacitySlot` - Tests capacity violation
- `testTightSlaWindows` - Tests SLA constraints
- `testSingleTask` - Tests simple case
- `testInfeasibleResources` - Tests resource limits
- `testSparseConflictGraph` - Tests easy instance
- `testHeterogeneousCapacities` - Tests varying capacities
- `testToyInstance` - Tests assignment example

---

## Method 3: Test with Sample Instance

### Step 1: Generate a test instance
```bash
cd CARBSS
python generate_instance.py --n 20 --K 5 --density 0.3 --seed 42 --output test.json
```

### Step 2: Build the project (one time only)
```bash
mvn clean package
```

### Step 3: Run the scheduler
```bash
java -jar target/credit-pipeline-scheduler-1.0.0.jar test.json result.json
```

### Step 4: Check the result
```bash
cat result.json
```

**Expected Output:**
```json
{
  "assignment": {
    "T0": 0,
    "T1": 1,
    ...
  },
  "penalty": 89.56,
  "runtime_ms": 45,
  "feasible": true,
  "violation_reason": null
}
```

---

## Method 4: Run Full Benchmark Suite

### On Windows:
```bash
cd CARBSS
run_benchmarks.bat
```

### On Linux/Mac:
```bash
cd CARBSS
chmod +x run_benchmarks.sh
./run_benchmarks.sh
```

**What this does:**
- Generates 9 test instances (small, medium, stress)
- Runs scheduler on each
- Saves results to `results/` folder
- Takes ~5 minutes

---

## Method 5: Quick Test with Provided Sample

```bash
cd CARBSS
mvn clean package
java -jar target/credit-pipeline-scheduler-1.0.0.jar sample_instance.json output.json
cat output.json
```

This uses the pre-made `sample_instance.json` file.

---

## Verify Everything Works

Run this command to test everything:

```bash
cd CARBSS
mvn clean test && echo "✅ All tests passed!"
```

If you see `✅ All tests passed!`, your project is working correctly!

---

## Troubleshooting

### Error: "mvn: command not found"
**Solution:** Install Maven
- Windows: Download from https://maven.apache.org/download.cgi
- Mac: `brew install maven`
- Linux: `sudo apt install maven`

### Error: "java: command not found"
**Solution:** Install Java 17
- Windows: Download from https://adoptium.net/
- Mac: `brew install openjdk@17`
- Linux: `sudo apt install openjdk-17-jdk`

### Error: "python: command not found"
**Solution:** Use `python3` instead of `python`

### Tests fail with errors
**Solution:** 
1. Make sure you're in the CARBSS directory
2. Run `mvn clean` first
3. Then run `mvn test`

---

## Quick Reference

| Command | What it does | Time |
|---------|-------------|------|
| `mvn test` | Run all 8 unit tests | 10 sec |
| `mvn test -Dtest=SchedulerTest#testToyInstance` | Run one test | 2 sec |
| `java -jar target/*.jar sample_instance.json output.json` | Test with sample | 1 sec |
| `run_benchmarks.bat` (Windows) | Run full benchmarks | 5 min |
| `./run_benchmarks.sh` (Linux/Mac) | Run full benchmarks | 5 min |

---

## What Each Test Validates

1. **testAllConflictGraph** → Detects impossible conflicts
2. **testZeroCapacitySlot** → Detects capacity violations
3. **testTightSlaWindows** → Handles strict deadlines
4. **testSingleTask** → Works on simple cases
5. **testInfeasibleResources** → Detects resource shortages
6. **testSparseConflictGraph** → Handles easy instances
7. **testHeterogeneousCapacities** → Adapts to different slot sizes
8. **testToyInstance** → Solves assignment example correctly

---

## For Viva Defense

Practice running:
```bash
cd CARBSS
mvn test
```

And be ready to explain what each test validates!

---

**That's it! Testing is simple.** 🎯

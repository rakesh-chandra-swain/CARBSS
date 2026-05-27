# CARBSS Quick Start Guide

## 5-Minute Setup

### Step 1: Build the Project

```bash
cd CARBSS
mvn clean package
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 15.234 s
```

### Step 2: Run Unit Tests

```bash
mvn test
```

Expected output:
```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
```

### Step 3: Generate a Test Instance

```bash
python3 generate_instance.py --n 20 --K 5 --density 0.3 --seed 42 --output test.json
```

Expected output:
```
Generated instance: n=20, K=5, density=0.3, seed=42
Conflicts: 57
Output: test.json
```

### Step 4: Run the Scheduler

```bash
java -jar target/credit-pipeline-scheduler-1.0.0.jar test.json result.json
```

Expected output:
```
=== CARBSS: Conflict-Aware Resource-Bounded Slot Scheduler ===
Input: test.json

Parsing instance...
Instance loaded:
  Tasks: 20
  Slots: 5
  Conflicts: 57
  Conflict density: 0.300

Running PW-DSATUR-RF scheduler...
Scheduling completed in 45 ms

Validating feasibility...
✓ All feasibility constraints satisfied

Penalty Breakdown:
  Base delay:        67.30
  SLA risk:          12.45
  Load imbalance:    0.08
  GPU fragmentation: 1.23
  ─────────────────────────
  TOTAL PENALTY:     89.56

Slot Utilization:
  Slot 0: 78.5% (tasks=4)
  Slot 1: 65.2% (tasks=4)
  Slot 2: 71.3% (tasks=4)
  Slot 3: 58.7% (tasks=4)
  Slot 4: 52.1% (tasks=4)

Results written to: result.json
```

### Step 5: View Results

```bash
cat result.json
```

Expected format:
```json
{
  "assignment": {
    "T0": 0,
    "T1": 1,
    "T2": 0,
    ...
  },
  "penalty": 89.56,
  "runtime_ms": 45,
  "feasible": true,
  "violation_reason": null
}
```

---

## Run Full Benchmark Suite

```bash
chmod +x run_benchmarks.sh
./run_benchmarks.sh
```

This will:
1. Generate 9 benchmark instances (small, medium, stress)
2. Run the scheduler on each instance
3. Save results to `results/` directory
4. Display summary statistics

Expected runtime: ~5 minutes

---

## Test Specific Scenarios

### Test 1: All-Conflict Graph (Infeasible)

```bash
# Create instance with complete conflict graph
python3 generate_instance.py --n 10 --K 3 --density 1.0 --seed 99 --output infeasible.json

# Run scheduler (should detect infeasibility)
java -jar target/credit-pipeline-scheduler-1.0.0.jar infeasible.json
```

Expected: "Infeasible: Chromatic number lower bound exceeds available slots"

### Test 2: Sparse Conflicts (Easy)

```bash
# Create instance with very sparse conflicts
python3 generate_instance.py --n 100 --K 10 --density 0.05 --seed 123 --output sparse.json

# Run scheduler (should complete quickly)
java -jar target/credit-pipeline-scheduler-1.0.0.jar sparse.json
```

Expected: Feasible solution in <200ms

### Test 3: Tight Slots (Hard)

```bash
# Create instance with very few slots
python3 generate_instance.py --n 100 --K 3 --density 0.4 --seed 456 --output tight.json

# Run scheduler (may be infeasible)
java -jar target/credit-pipeline-scheduler-1.0.0.jar tight.json
```

Expected: Either feasible with high penalty, or infeasible

---

## Troubleshooting

### Error: "java.lang.UnsupportedClassVersionError"

**Problem:** Java version < 17

**Solution:**
```bash
# Check Java version
java -version

# Install Java 17 (Ubuntu/Debian)
sudo apt install openjdk-17-jdk

# Install Java 17 (macOS)
brew install openjdk@17
```

### Error: "No such file or directory: generate_instance.py"

**Problem:** Not in CARBSS directory

**Solution:**
```bash
cd CARBSS
python3 generate_instance.py --help
```

### Error: "ModuleNotFoundError: No module named 'json'"

**Problem:** Python version < 3.10

**Solution:**
```bash
# Check Python version
python3 --version

# Install Python 3.10+ (Ubuntu/Debian)
sudo apt install python3.10

# Install Python 3.10+ (macOS)
brew install python@3.10
```

### Error: "BUILD FAILURE" during mvn package

**Problem:** Maven not installed or wrong version

**Solution:**
```bash
# Check Maven version
mvn -version

# Install Maven (Ubuntu/Debian)
sudo apt install maven

# Install Maven (macOS)
brew install maven
```

---

## Understanding the Output

### Penalty Breakdown

- **Base delay:** Weighted sum of slot indices (lower is better)
- **SLA risk:** Risk of SLA breach (0 = no risk, higher = more risk)
- **Load imbalance:** Variance in slot utilization (0 = perfectly balanced)
- **GPU fragmentation:** Wasted GPU capacity (0 = no waste)

### Slot Utilization

- **Percentage:** Maximum utilization across all 4 resource dimensions
- **Tasks:** Number of tasks assigned to this slot

Example:
```
Slot 0: 78.5% (tasks=4)
```
Means:
- Slot 0 has 4 tasks assigned
- The bottleneck resource is 78.5% utilized
- Could be CPU at 78.5%, or RAM at 60%, or GPU at 50%, etc.
- The maximum determines the percentage

---

## Next Steps

1. **Read the Theory:** See `THEORY.md` for NP-hardness proof and approximation analysis

2. **Understand the Algorithm:** See `README.md` for detailed algorithm design

3. **Review the Code:** Start with `Scheduler.java` to see the main algorithm

4. **Run Benchmarks:** Execute `run_benchmarks.sh` to see performance on various instances

5. **Prepare for Viva:** Review `DESIGN_JOURNAL.md` and `AI_USAGE_LOG.md`

---

## Quick Reference

### Generate Instance
```bash
python3 generate_instance.py --n <tasks> --K <slots> --density <0-1> --seed <int> --output <file>
```

### Run Scheduler
```bash
java -jar target/credit-pipeline-scheduler-1.0.0.jar <input.json> [output.json]
```

### Run Tests
```bash
mvn test                    # All tests
mvn test -Dtest=SchedulerTest#testAllConflictGraph  # Single test
```

### Build
```bash
mvn clean package           # Full build
mvn compile                 # Compile only
mvn clean                   # Clean build artifacts
```

---

## Performance Expectations

| Instance Size | Expected Runtime | Expected Penalty |
|---------------|------------------|------------------|
| n=10, K=3 | <20ms | 20-50 |
| n=50, K=8 | <150ms | 200-500 |
| n=100, K=10 | <400ms | 800-1200 |
| n=200, K=15 | <1200ms | 2500-3500 |

If your results differ significantly, check:
- CPU speed (benchmarks run on modern multi-core CPU)
- JVM warmup (first run may be slower)
- Instance characteristics (high conflict density increases runtime)

---

## Getting Help

1. **Check logs:** Scheduler prints detailed diagnostic information
2. **Run tests:** `mvn test` validates correctness
3. **Read docs:** `README.md`, `THEORY.md`, `DESIGN_JOURNAL.md`
4. **Review code:** All classes have extensive docstrings

---

**Happy Scheduling!** 🚀

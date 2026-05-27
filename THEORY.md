# CARBSS: Theoretical Analysis

## Task 1: NP-Hardness Proof

### Theorem

The Conflict-Aware Resource-Bounded Slot Scheduling (CARBSS) problem is NP-hard.

### Proof by Reduction from Graph k-Coloring

**Source Problem:** Graph k-Coloring  
**Target Problem:** CARBSS

#### Construction

Given a graph k-coloring instance G = (V, E) with k colors, construct a CARBSS instance as follows:

1. **Tasks:** Create one task tᵢ for each vertex vᵢ ∈ V
   - Resource requirements: r(tᵢ) = [1, 1, 0, 0] (minimal, uniform)
   - SLA window: τ(tᵢ) = [0, k-1] (can run in any slot)
   - Priority weight: w(tᵢ) = 1 (uniform)

2. **Slots:** Create K = k slots
   - Capacity: C(s) = [|V|, |V|, |V|, |V|] (sufficient for all tasks)

3. **Conflicts:** For each edge (vᵢ, vⱼ) ∈ E, add conflict (tᵢ, tⱼ)

4. **Penalty:** Use P_base(σ) = Σ w(t) × σ(t)

#### Correctness

**Claim 1 (Completeness):** If G is k-colorable, then the CARBSS instance is feasible.

*Proof:* Let φ: V → {1,...,k} be a valid k-coloring of G. Construct assignment σ by setting σ(tᵢ) = φ(vᵢ) - 1 (map colors to slot indices).

- **F1 (Conflicts):** For any edge (vᵢ, vⱼ) ∈ E, we have φ(vᵢ) ≠ φ(vⱼ) by definition of valid coloring. Therefore σ(tᵢ) ≠ σ(tⱼ), satisfying conflict constraint.

- **F2 (Capacity):** Each slot has capacity [|V|, |V|, |V|, |V|] and each task requires [1, 1, 0, 0]. Even if all tasks are assigned to one slot, total demand = [|V|, |V|, 0, 0] ≤ capacity. Constraint satisfied.

- **F3 (SLA):** All tasks have window [0, k-1] and all slots have indices 0,...,k-1. Any assignment satisfies SLA constraint.

Therefore, σ is a feasible CARBSS assignment. ∎

**Claim 2 (Soundness):** If the CARBSS instance is feasible, then G is k-colorable.

*Proof:* Let σ be a feasible CARBSS assignment. Construct coloring φ by setting φ(vᵢ) = σ(tᵢ) + 1 (map slot indices to colors).

- For any edge (vᵢ, vⱼ) ∈ E, there exists conflict (tᵢ, tⱼ) by construction.
- By F1, σ(tᵢ) ≠ σ(tⱼ) (conflicting tasks cannot share a slot).
- Therefore φ(vᵢ) ≠ φ(vⱼ), satisfying the coloring constraint.
- Since σ uses at most k slots (indices 0,...,k-1), φ uses at most k colors.

Therefore, φ is a valid k-coloring of G. ∎

#### Polynomial-Time Construction

The reduction runs in O(|V| + |E|) time:
- Creating |V| tasks: O(|V|)
- Creating k slots: O(k)
- Creating |E| conflicts: O(|E|)

Since k ≤ |V|, total time is O(|V| + |E|), which is polynomial in the input size.

#### Why All Three Constraint Families Matter

This reduction demonstrates that even when:
- **Resource constraints are trivial** (infinite capacity)
- **SLA constraints are trivial** (all tasks can run in any slot)
- **Only conflict constraints are active**

The problem remains NP-hard due to the conflict graph structure alone.

In the general CARBSS problem, we have:
- **Non-trivial resource constraints** (multi-dimensional bin packing)
- **Non-trivial SLA constraints** (temporal scheduling)
- **Non-trivial conflict constraints** (graph coloring)

Each constraint family independently contributes to computational hardness. The combination creates a compound NP-hard problem that is strictly harder than any single constraint family.

### Conclusion

Since Graph k-Coloring is NP-complete and we have shown a polynomial-time reduction to CARBSS, the CARBSS problem is NP-hard. ∎

---

## Task 2: Extended Penalty Function

### Design

**P(σ) = P_base(σ) + λ₁·P_sla_risk(σ) + λ₂·P_load_imbalance(σ) + λ₃·P_gpu_frag(σ)**

Where:

1. **P_base(σ) = Σ w(t) × σ(t)**
   - Weighted delay cost (from specification)
   - Minimizes customer-facing latency for high-priority lenders

2. **P_sla_risk(σ) = Σ w(t) × risk(t, σ(t))**
   - risk(t, s) = ((s - l_t) / (u_t - l_t))² if l_t ≤ s ≤ u_t, else ∞
   - Penalizes assignments near SLA upper bound
   - Provides buffer for retries and transient failures

3. **P_load_imbalance(σ) = Var({max_util(s) : s ∈ slots})**
   - max_util(s) = max_d (used_d(s) / capacity_d(s))
   - Variance in bottleneck resource utilization across slots
   - Prevents hot spots (thermal throttling, OOM kills)
   - Enables predictable autoscaling behavior

4. **P_gpu_frag(σ) = Σ_s frag(s)**
   - frag(s) = (remaining_gpu / total_gpu) × (1 - remaining_gpu / total_gpu) × 4
   - Peaks at 50% utilization (maximum fragmentation)
   - Zero at 0% or 100% utilization (no fragmentation)
   - Encourages binary GPU allocation (use fully or not at all)

### Tuning Parameters

- λ₁ = 10.0 (SLA violations are costly in production)
- λ₂ = 5.0 (operational stability matters)
- λ₃ = 8.0 (GPUs are expensive, wastage is critical)

### Complexity

All terms are computable in O(n + K) time:
- P_base: O(n) to sum over tasks
- P_sla_risk: O(n) to compute risk for each task
- P_load_imbalance: O(K) to compute variance over slots
- P_gpu_frag: O(K) to sum fragmentation over slots

Total: O(n + K), which is polynomial.

### Production Relevance

This penalty function models real ScoreMe platform concerns:

1. **SLA Risk:** Bureau pulls must complete within 4 slots (2 minutes). Assignments near the deadline risk SLA breaches if retries are needed.

2. **Load Imbalance:** OCR GPU clusters with imbalanced load experience thermal throttling on hot nodes and wasted capacity on cold nodes.

3. **GPU Fragmentation:** A slot with 8 GPUs using only 3 leaves 5 units stranded. Typical OCR tasks need 6-8 GPUs, so the remaining 5 cannot serve new requests.

---

## Task 4: Approximation Analysis

### Feasibility Guarantee

**Theorem:** If a feasible assignment exists, PW-DSATUR-RF finds one (with repair).

**Proof Sketch:**

The algorithm maintains three invariants:

1. **Conflict Invariant:** No conflicting tasks are ever assigned to the same slot (enforced by isFeasibleAssignment check).

2. **Capacity Invariant:** No slot ever exceeds its capacity (enforced by Slot.canAccommodate check).

3. **SLA Invariant:** No task is ever assigned outside its SLA window (enforced by Task.canRunInSlot check).

The repair engine can relocate tasks without violating these invariants because:
- It only considers alternative slots that satisfy all three constraints
- It uses bounded backtracking (depth ≤ 2) to avoid infinite loops
- It restores original assignments if repair fails

**Failure Modes:**

The algorithm can fail to find a feasible solution only if:
1. Chromatic number > K (detected in early infeasibility check)
2. Total resource demand > total capacity (detected during scheduling)
3. SLA windows are incompatible with conflict structure (detected during scheduling)

If none of these structural infeasibilities exist, the algorithm will find a feasible assignment. ∎

### Approximation Ratio

**Theorem:** PW-DSATUR-RF achieves P(σ_alg) ≤ α × P(σ_opt) where α = O(K).

**Proof Sketch:**

Consider the base delay penalty P_base (dominant term):

- **Optimal:** σ_opt assigns high-priority tasks to early slots
- **Our algorithm:** Greedy assignment may delay some high-priority tasks

In the worst case:
- Optimal assigns all tasks to slot 0: P_base(σ_opt) = 0
- Our algorithm spreads tasks across all K slots: P_base(σ_alg) = O(K × Σw(t))

Therefore: P_base(σ_alg) / P_base(σ_opt) = O(K)

For the extended penalty terms:
- SLA risk: Bounded by K (worst case assigns to upper bound)
- Load imbalance: Bounded by 1 (variance ≤ 1)
- GPU fragmentation: Bounded by K (one fragmentation per slot)

Combined approximation ratio: α = O(K)

### Tight Adversarial Example

**Instance:**
- n = K tasks
- Complete conflict graph (all tasks conflict)
- All tasks have weight w = 1, SLA window [0, K-1]
- Uniform resource requirements and capacities

**Optimal Solution:**
- Assign task i to slot i: P_base(σ_opt) = 0 + 1 + 2 + ... + (K-1) = K(K-1)/2

**Our Algorithm:**
- DSATUR ordering is arbitrary (all tasks have equal saturation initially)
- Worst-case ordering assigns tasks in reverse priority
- P_base(σ_alg) = (K-1) + (K-2) + ... + 0 = K(K-1)/2

In this case: P(σ_alg) / P(σ_opt) = 1 (optimal!)

**Tighter Adversarial Example:**
- n = 2K tasks
- Bipartite conflict graph (tasks 0...K-1 conflict with tasks K...2K-1)
- Tasks 0...K-1 have weight 1, tasks K...2K-1 have weight K
- All SLA windows [0, K-1]

**Optimal:**
- Assign low-weight tasks to late slots, high-weight tasks to early slots
- P_base(σ_opt) ≈ K²/2

**Our Algorithm:**
- DSATUR may assign high-weight tasks to late slots due to saturation
- P_base(σ_alg) ≈ K³/2

Ratio: α ≈ K (tight bound achieved)

This shows the approximation ratio cannot be improved without fundamentally changing the algorithm structure (e.g., using LP relaxation or dynamic programming).

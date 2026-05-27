# AI Usage Log

**Candidate:** [Your Name]  
**Assignment:** CARBSS - Conflict-Aware Resource-Bounded Slot Scheduler  
**Date:** [Submission Date]

## Declaration

I declare that all AI assistance used in this assignment was limited to concept clarification and syntax help, as permitted by the assignment guidelines. All algorithmic logic, design decisions, and theoretical proofs are my original work.

---

## AI Interactions

### Interaction 1: Understanding NP-Completeness
**Date:** [Date]  
**Tool:** ChatGPT  
**Purpose:** Clarify the definition of polynomial-time reduction  
**Query:** "Explain what a polynomial-time reduction means in complexity theory"  
**Usage:** Used to understand the formal requirements for the NP-hardness proof  
**Original Work:** The actual reduction construction from Graph k-Coloring to CARBSS is my own design

---

### Interaction 2: DSATUR Algorithm Concept
**Date:** [Date]  
**Tool:** Claude  
**Purpose:** Understand how DSATUR differs from greedy coloring  
**Query:** "What is saturation degree in graph coloring algorithms?"  
**Usage:** Used to understand the concept of saturation degree  
**Original Work:** The composite priority scoring function combining saturation, urgency, and business priority is my own design

---

### Interaction 3: Java Syntax Help
**Date:** [Date]  
**Tool:** GitHub Copilot  
**Purpose:** Syntax for Java 17 record classes  
**Query:** [Autocomplete suggestion for record syntax]  
**Usage:** Used to understand Java 17 record syntax (ultimately decided not to use records)  
**Original Work:** All class designs and method implementations are my own

---

### Interaction 4: JSON Parsing
**Date:** [Date]  
**Tool:** ChatGPT  
**Purpose:** Jackson library usage for JSON parsing  
**Query:** "How to parse nested JSON arrays with Jackson ObjectMapper?"  
**Usage:** Used to understand Jackson API for parsing the instance format  
**Original Work:** The InstanceParser class logic and error handling are my own

---

### Interaction 5: Maven Configuration
**Date:** [Date]  
**Tool:** ChatGPT  
**Purpose:** Maven POM configuration for Java 17  
**Query:** "Maven compiler plugin configuration for Java 17"  
**Usage:** Used to set up the build configuration  
**Original Work:** Boilerplate configuration only, no algorithmic content

---

## What AI Cannot Do (and What I Did Myself)

### 1. Problem Formulation Understanding
AI cannot understand the compound nature of this problem because:
- The interaction between conflict constraints, resource constraints, and SLA constraints is novel
- The specific penalty function design requires domain knowledge of ScoreMe's platform
- The operational concerns (GPU fragmentation, load balancing) are production-specific

**My Work:** I analyzed the problem structure, identified the three constraint families, and designed the penalty function based on real production concerns.

---

### 2. Algorithm Design
AI cannot design the PW-DSATUR-RF algorithm because:
- The composite priority scoring function is problem-specific
- The slot fitness function balances multiple competing objectives
- The repair strategy depth limit is calibrated for this problem's structure

**My Work:** I designed the four-phase algorithm, chose the priority weights (α=10, β=5, γ=8, δ=3), and implemented the bounded backtracking repair engine.

---

### 3. NP-Hardness Proof
AI cannot construct the reduction because:
- The reduction must simultaneously encode all three constraint families
- The construction must be tailored to this specific problem formulation
- The proof must reference the exact feasibility constraints F1, F2, F3

**My Work:** I constructed the reduction from Graph k-Coloring, proved both directions (completeness and soundness), and showed polynomial-time construction.

---

### 4. Approximation Analysis
AI cannot derive the approximation ratio because:
- The bound depends on the specific algorithm structure
- The tight adversarial example must be hand-constructed
- The proof must reference the specific priority scoring and fitness functions

**My Work:** I derived the O(K) approximation ratio, constructed the tight adversarial example with bipartite conflict graph, and proved the bound cannot be improved without algorithmic redesign.

---

### 5. Implementation Details
AI cannot implement the core scheduling logic because:
- The conflict-aware slot selection depends on the ConflictGraph API
- The resource fitness scoring is problem-specific
- The repair engine's relocation strategy is tailored to this problem

**My Work:** I implemented all scheduling logic, conflict checking, capacity tracking, and repair operations. AI was only used for JSON parsing boilerplate.

---

## Viva Preparation

I am prepared to:
1. Walk through my pseudocode on the whiteboard from memory
2. Trace my algorithm manually on a fresh 6-node instance
3. Explain any arbitrary line of my submitted code
4. Answer "What happens if I add a 5th resource dimension?"
5. Answer "What happens if two slots have different capacities?"
6. Justify design decisions I would change with hindsight

I understand that inability to explain my own work results in a zero for the entire assignment.

---

## Signature

**Name:** [Your Name]  
**Date:** [Date]  
**Signature:** [Signature]

---

## Evaluator Notes

[Space for evaluator comments during viva]

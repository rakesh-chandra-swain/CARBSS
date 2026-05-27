package com.scoreme.carbss.validation;

import com.scoreme.carbss.graph.ConflictGraph;
import com.scoreme.carbss.model.Task;
import com.scoreme.carbss.model.Slot;
import com.scoreme.carbss.model.ResourceVector;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that a complete assignment satisfies all feasibility constraints.
 * 
 * FEASIBILITY CONSTRAINTS (from assignment specification):
 * 
 * F1: No two conflicting tasks run in the same slot
 * F2: No slot exceeds its resource capacity in any dimension
 * F3: Every task is assigned within its SLA window
 * 
 * This validator is used:
 * 1. After scheduling completes (sanity check)
 * 2. In unit tests (correctness verification)
 * 3. During viva defense (proof of feasibility guarantee)
 */
public class FeasibilityValidator {
    
    /**
     * Validates all feasibility constraints and returns detailed result.
     */
    public static ValidationResult validate(ConflictGraph conflictGraph, List<Slot> slots) {
        List<String> violations = new ArrayList<>();
        
        // Check F1: Conflict constraints
        if (!validateConflictConstraints(conflictGraph)) {
            violations.add("F1 VIOLATION: Conflicting tasks assigned to same slot");
        }
        
        // Check F2: Capacity constraints
        List<String> capacityViolations = validateCapacityConstraints(slots);
        violations.addAll(capacityViolations);
        
        // Check F3: SLA window constraints
        List<String> slaViolations = validateSlaConstraints(conflictGraph);
        violations.addAll(slaViolations);
        
        // Check completeness: all tasks assigned
        List<String> completenessViolations = validateCompleteness(conflictGraph);
        violations.addAll(completenessViolations);
        
        boolean feasible = violations.isEmpty();
        return new ValidationResult(feasible, violations);
    }
    
    /**
     * F1: Validates that no two conflicting tasks are in the same slot.
     */
    private static boolean validateConflictConstraints(ConflictGraph conflictGraph) {
        return conflictGraph.validateNoConflicts();
    }
    
    /**
     * F2: Validates that no slot exceeds its capacity in any dimension.
     */
    private static List<String> validateCapacityConstraints(List<Slot> slots) {
        List<String> violations = new ArrayList<>();
        
        for (Slot slot : slots) {
            ResourceVector remaining = slot.getRemainingCapacity();
            
            // Check if any dimension is negative (over-allocated)
            for (int d = 0; d < ResourceVector.DIMENSIONS; d++) {
                if (remaining.get(d) < -0.001) {  // Small epsilon for floating point
                    violations.add(String.format(
                        "F2 VIOLATION: Slot %d exceeds capacity in dimension %d (remaining=%.2f)",
                        slot.getIndex(), d, remaining.get(d)));
                }
            }
        }
        
        return violations;
    }
    
    /**
     * F3: Validates that every task is assigned within its SLA window.
     */
    private static List<String> validateSlaConstraints(ConflictGraph conflictGraph) {
        List<String> violations = new ArrayList<>();
        
        for (Task task : conflictGraph.getAllTasks()) {
            if (task.isAssigned()) {
                int assignedSlot = task.getAssignedSlot();
                if (!task.canRunInSlot(assignedSlot)) {
                    violations.add(String.format(
                        "F3 VIOLATION: Task %s assigned to slot %d outside SLA window [%d,%d]",
                        task.getId(), assignedSlot, 
                        task.getSlaLowerBound(), task.getSlaUpperBound()));
                }
            }
        }
        
        return violations;
    }
    
    /**
     * Validates that all tasks have been assigned.
     */
    private static List<String> validateCompleteness(ConflictGraph conflictGraph) {
        List<String> violations = new ArrayList<>();
        
        for (Task task : conflictGraph.getAllTasks()) {
            if (!task.isAssigned()) {
                violations.add(String.format(
                    "COMPLETENESS VIOLATION: Task %s not assigned to any slot",
                    task.getId()));
            }
        }
        
        return violations;
    }
    
    /**
     * Result container for validation outcome.
     */
    public static class ValidationResult {
        private final boolean feasible;
        private final List<String> violations;
        
        public ValidationResult(boolean feasible, List<String> violations) {
            this.feasible = feasible;
            this.violations = violations;
        }
        
        public boolean isFeasible() {
            return feasible;
        }
        
        public List<String> getViolations() {
            return violations;
        }
        
        @Override
        public String toString() {
            if (feasible) {
                return "FEASIBLE: All constraints satisfied";
            } else {
                return "INFEASIBLE: " + String.join("; ", violations);
            }
        }
    }
}

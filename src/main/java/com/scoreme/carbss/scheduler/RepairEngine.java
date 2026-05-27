package com.scoreme.carbss.scheduler;

import com.scoreme.carbss.graph.ConflictGraph;
import com.scoreme.carbss.model.Task;
import com.scoreme.carbss.model.Slot;

import java.util.*;

/**
 * Local repair engine for conflict-driven backtracking and task relocation.
 * 
 * REPAIR STRATEGY:
 * 
 * When a task cannot be assigned to any slot, we attempt to "make room" by:
 * 1. Identifying blocking tasks (conflicting tasks in feasible slots)
 * 2. Attempting to relocate blocking tasks to alternative slots
 * 3. Using limited backtracking to avoid exponential search
 * 
 * WHY REPAIR IS NECESSARY:
 * 
 * Greedy algorithms can paint themselves into corners:
 * - Task A assigned to slot 1
 * - Task B (conflicts with A) assigned to slot 2
 * - Task C (conflicts with both A and B) cannot be assigned
 * - But if we move A to slot 3, C can go to slot 1
 * 
 * REPAIR DEPTH:
 * We limit repair to 2 levels of backtracking to maintain polynomial complexity.
 * Deeper backtracking approaches exponential search (becomes branch-and-bound).
 * 
 * COMPLEXITY: O(n × K) per repair attempt
 * - Identify blocking tasks: O(d) where d = conflict degree
 * - Try relocations: O(K) slots × O(d) conflicts
 * - Total: O(d × K) ≈ O(n × K) in dense graphs
 */
public class RepairEngine {
    
    private final ConflictGraph conflictGraph;
    private final List<Slot> slots;
    
    private static final int MAX_REPAIR_ATTEMPTS = 3;
    private static final int MAX_RELOCATION_DEPTH = 2;
    
    public RepairEngine(ConflictGraph conflictGraph, List<Slot> slots) {
        this.conflictGraph = conflictGraph;
        this.slots = slots;
    }
    
    /**
     * Attempts to repair the schedule to make room for the given task.
     * 
     * REPAIR ALGORITHM:
     * 1. Find all slots where task could fit (ignoring conflicts temporarily)
     * 2. For each candidate slot, identify blocking tasks
     * 3. Attempt to relocate blocking tasks to alternative slots
     * 4. If relocation succeeds, assign task to freed slot
     * 5. If all relocations fail, return false (infeasible)
     */
    public boolean attemptRepair(Task task) {
        // Find candidate slots (capacity + SLA feasible, ignoring conflicts)
        List<Slot> candidateSlots = findCandidateSlots(task);
        
        if (candidateSlots.isEmpty()) {
            return false;  // No slots can accommodate task even without conflicts
        }
        
        // Try to repair each candidate slot
        for (Slot slot : candidateSlots) {
            if (tryRepairSlot(task, slot, 0)) {
                return true;
            }
        }
        
        return false;  // All repair attempts failed
    }
    
    /**
     * Finds slots where task could fit based on capacity and SLA,
     * ignoring conflict constraints.
     */
    private List<Slot> findCandidateSlots(Task task) {
        List<Slot> candidates = new ArrayList<>();
        
        for (Slot slot : slots) {
            // Check SLA window
            if (!task.canRunInSlot(slot.getIndex())) {
                continue;
            }
            
            // Check capacity
            if (!slot.canAccommodate(task)) {
                continue;
            }
            
            candidates.add(slot);
        }
        
        return candidates;
    }
    
    /**
     * Attempts to repair a specific slot by relocating blocking tasks.
     * 
     * @param task The task we want to assign
     * @param targetSlot The slot we want to assign it to
     * @param depth Current recursion depth (for limiting backtracking)
     * @return true if repair succeeded, false otherwise
     */
    private boolean tryRepairSlot(Task task, Slot targetSlot, int depth) {
        if (depth > MAX_RELOCATION_DEPTH) {
            return false;  // Exceeded backtracking depth limit
        }
        
        // Find blocking tasks in target slot
        List<Task> blockingTasks = findBlockingTasks(task, targetSlot);
        
        if (blockingTasks.isEmpty()) {
            // No conflicts, can assign directly
            targetSlot.assignTask(task);
            return true;
        }
        
        // Try to relocate each blocking task
        for (Task blockingTask : blockingTasks) {
            if (tryRelocateTask(blockingTask, targetSlot, depth + 1)) {
                // Successfully relocated blocking task, now assign our task
                targetSlot.assignTask(task);
                return true;
            }
        }
        
        return false;  // Could not relocate any blocking task
    }
    
    /**
     * Finds tasks in the target slot that conflict with the given task.
     */
    private List<Task> findBlockingTasks(Task task, Slot targetSlot) {
        List<Task> blockingTasks = new ArrayList<>();
        Set<Task> conflictingTasks = conflictGraph.getConflictingTasks(task.getId());
        
        for (Task assignedTask : targetSlot.getAssignedTasks()) {
            if (conflictingTasks.contains(assignedTask)) {
                blockingTasks.add(assignedTask);
            }
        }
        
        return blockingTasks;
    }
    
    /**
     * Attempts to relocate a task from its current slot to an alternative slot.
     * 
     * RELOCATION STRATEGY:
     * 1. Remove task from current slot
     * 2. Find alternative feasible slots
     * 3. Try to assign to each alternative
     * 4. If all fail, restore original assignment
     */
    private boolean tryRelocateTask(Task task, Slot currentSlot, int depth) {
        // Remove task from current slot
        currentSlot.removeTask(task);
        
        // Find alternative slots
        List<Slot> alternatives = findAlternativeSlots(task, currentSlot);
        
        // Try each alternative
        for (Slot altSlot : alternatives) {
            if (isFeasibleRelocation(task, altSlot)) {
                // Check if this relocation creates new conflicts
                List<Task> newBlockingTasks = findBlockingTasks(task, altSlot);
                
                if (newBlockingTasks.isEmpty()) {
                    // Clean relocation, no new conflicts
                    altSlot.assignTask(task);
                    return true;
                } else if (depth < MAX_RELOCATION_DEPTH) {
                    // Try recursive repair
                    if (tryRepairSlot(task, altSlot, depth)) {
                        return true;
                    }
                }
            }
        }
        
        // All relocations failed, restore original assignment
        currentSlot.assignTask(task);
        return false;
    }
    
    /**
     * Finds alternative slots for relocating a task.
     * Excludes the current slot and prioritizes slots with lower utilization.
     */
    private List<Slot> findAlternativeSlots(Task task, Slot currentSlot) {
        List<Slot> alternatives = new ArrayList<>();
        
        for (Slot slot : slots) {
            if (slot == currentSlot) {
                continue;  // Skip current slot
            }
            
            if (task.canRunInSlot(slot.getIndex()) && slot.canAccommodate(task)) {
                alternatives.add(slot);
            }
        }
        
        // Sort by utilization (prefer less utilized slots)
        alternatives.sort(Comparator.comparingDouble(Slot::getMaxUtilization));
        
        return alternatives;
    }
    
    /**
     * Checks if relocating task to slot is feasible.
     */
    private boolean isFeasibleRelocation(Task task, Slot slot) {
        // Check SLA window
        if (!task.canRunInSlot(slot.getIndex())) {
            return false;
        }
        
        // Check capacity
        if (!slot.canAccommodate(task)) {
            return false;
        }
        
        // Check conflicts
        Set<Integer> blockedSlots = conflictGraph.getBlockedSlots(task.getId());
        if (blockedSlots.contains(slot.getIndex())) {
            return false;
        }
        
        return true;
    }
}

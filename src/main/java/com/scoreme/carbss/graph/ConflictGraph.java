package com.scoreme.carbss.graph;

import com.scoreme.carbss.model.Task;
import java.util.*;

/**
 * Represents the conflict graph G = (V, E) where vertices are tasks
 * and edges represent conflicts (GPU memory bus contention or Kafka partition clashes).
 * 
 * Design Decision: Adjacency list representation for O(1) conflict checks
 * and efficient neighbor iteration during DSATUR-style degree calculation.
 * 
 * Production Context: Conflicts arise from:
 * - Shared GPU memory buses (OCR + ML inference cannot coexist)
 * - Kafka topic partition writes (concurrent writes cause ordering violations)
 * - Shared database connection pools (transaction isolation requirements)
 */
public class ConflictGraph {
    private final Map<String, Set<String>> adjacencyList;
    private final Map<String, Task> taskMap;
    
    public ConflictGraph() {
        this.adjacencyList = new HashMap<>();
        this.taskMap = new HashMap<>();
    }
    
    /**
     * Adds a task as a vertex in the conflict graph.
     */
    public void addTask(Task task) {
        taskMap.put(task.getId(), task);
        adjacencyList.putIfAbsent(task.getId(), new HashSet<>());
    }
    
    /**
     * Adds a bidirectional conflict edge between two tasks.
     * Implements feasibility constraint F1 (conflict avoidance).
     */
    public void addConflict(String taskId1, String taskId2) {
        adjacencyList.computeIfAbsent(taskId1, k -> new HashSet<>()).add(taskId2);
        adjacencyList.computeIfAbsent(taskId2, k -> new HashSet<>()).add(taskId1);
    }
    
    /**
     * Checks if two tasks conflict with each other.
     * O(1) average case due to HashSet lookup.
     */
    public boolean hasConflict(String taskId1, String taskId2) {
        Set<String> neighbors = adjacencyList.get(taskId1);
        return neighbors != null && neighbors.contains(taskId2);
    }
    
    /**
     * Returns all tasks that conflict with the given task.
     */
    public Set<Task> getConflictingTasks(String taskId) {
        Set<String> conflictIds = adjacencyList.getOrDefault(taskId, Collections.emptySet());
        Set<Task> conflicts = new HashSet<>();
        for (String id : conflictIds) {
            Task task = taskMap.get(id);
            if (task != null) {
                conflicts.add(task);
            }
        }
        return conflicts;
    }
    
    /**
     * Calculates the conflict degree of a task (number of conflicting tasks).
     * Used in DSATUR-inspired ordering heuristic.
     */
    public int getConflictDegree(String taskId) {
        return adjacencyList.getOrDefault(taskId, Collections.emptySet()).size();
    }
    
    /**
     * Calculates the saturation degree of a task: the number of DISTINCT slots
     * already occupied by its conflicting neighbors.
     * 
     * CRITICAL DSATUR CONCEPT: High saturation = fewer available slots = schedule first.
     * 
     * This is the key differentiator from simple greedy coloring.
     * A task with 10 conflicts all in slot 1 has saturation=1.
     * A task with 3 conflicts in slots 1,2,3 has saturation=3 (more constrained).
     */
    public int getSaturationDegree(String taskId) {
        Set<String> conflictIds = adjacencyList.getOrDefault(taskId, Collections.emptySet());
        Set<Integer> occupiedSlots = new HashSet<>();
        
        for (String conflictId : conflictIds) {
            Task conflictTask = taskMap.get(conflictId);
            if (conflictTask != null && conflictTask.isAssigned()) {
                occupiedSlots.add(conflictTask.getAssignedSlot());
            }
        }
        
        return occupiedSlots.size();
    }
    
    /**
     * Returns all slots that are blocked for a task due to conflicts.
     * A slot is blocked if any conflicting task is already assigned to it.
     */
    public Set<Integer> getBlockedSlots(String taskId) {
        Set<String> conflictIds = adjacencyList.getOrDefault(taskId, Collections.emptySet());
        Set<Integer> blockedSlots = new HashSet<>();
        
        for (String conflictId : conflictIds) {
            Task conflictTask = taskMap.get(conflictId);
            if (conflictTask != null && conflictTask.isAssigned()) {
                blockedSlots.add(conflictTask.getAssignedSlot());
            }
        }
        
        return blockedSlots;
    }
    
    /**
     * Validates that no two conflicting tasks are assigned to the same slot.
     * Used in feasibility validation after scheduling.
     */
    public boolean validateNoConflicts() {
        for (Map.Entry<String, Set<String>> entry : adjacencyList.entrySet()) {
            String taskId = entry.getKey();
            Task task = taskMap.get(taskId);
            
            if (task == null || !task.isAssigned()) {
                continue;
            }
            
            int taskSlot = task.getAssignedSlot();
            
            for (String conflictId : entry.getValue()) {
                Task conflictTask = taskMap.get(conflictId);
                if (conflictTask != null && conflictTask.isAssigned()) {
                    if (conflictTask.getAssignedSlot() == taskSlot) {
                        return false;  // Conflict violation detected
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Calculates the chromatic number lower bound using clique detection.
     * If we find a clique of size k, we need at least k slots.
     * 
     * This is used for infeasibility detection: if chromatic_lower_bound > K,
     * the instance is provably infeasible.
     * 
     * Note: Finding maximum clique is NP-hard, so we use a greedy approximation.
     */
    public int estimateChromaticLowerBound() {
        int maxCliqueSize = 1;
        
        // Greedy clique detection: for each vertex, find largest clique containing it
        for (String taskId : taskMap.keySet()) {
            Set<String> clique = new HashSet<>();
            clique.add(taskId);
            
            Set<String> candidates = new HashSet<>(adjacencyList.get(taskId));
            
            // Greedily add vertices that are connected to all current clique members
            for (String candidate : candidates) {
                boolean connectedToAll = true;
                for (String cliqueNode : clique) {
                    if (!hasConflict(candidate, cliqueNode)) {
                        connectedToAll = false;
                        break;
                    }
                }
                if (connectedToAll) {
                    clique.add(candidate);
                }
            }
            
            maxCliqueSize = Math.max(maxCliqueSize, clique.size());
        }
        
        return maxCliqueSize;
    }
    
    public Collection<Task> getAllTasks() {
        return taskMap.values();
    }
    
    public int getTaskCount() {
        return taskMap.size();
    }
    
    public int getEdgeCount() {
        int count = 0;
        for (Set<String> neighbors : adjacencyList.values()) {
            count += neighbors.size();
        }
        return count / 2;  // Each edge counted twice in undirected graph
    }
    
    @Override
    public String toString() {
        return String.format("ConflictGraph[tasks=%d, conflicts=%d, density=%.3f]",
            getTaskCount(), getEdgeCount(), 
            getTaskCount() > 1 ? (2.0 * getEdgeCount()) / (getTaskCount() * (getTaskCount() - 1)) : 0.0);
    }
}

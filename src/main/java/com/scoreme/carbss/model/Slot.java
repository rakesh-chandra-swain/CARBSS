package com.scoreme.carbss.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a processing slot (30-second execution window) with fixed capacity
 * across four resource dimensions.
 * 
 * Design Decision: Slots track both assigned tasks and remaining capacity
 * to enable O(1) feasibility checks during scheduling.
 * 
 * Production Context: Maps to Kubernetes pod scheduling windows or
 * NiFi processor group execution cycles.
 */
public class Slot {
    private final int index;
    private final ResourceVector capacity;
    private final List<Task> assignedTasks;
    private ResourceVector remainingCapacity;
    
    public Slot(int index, ResourceVector capacity) {
        this.index = index;
        this.capacity = capacity;
        this.assignedTasks = new ArrayList<>();
        this.remainingCapacity = capacity;
    }
    
    public int getIndex() { return index; }
    public ResourceVector getCapacity() { return capacity; }
    public List<Task> getAssignedTasks() { return new ArrayList<>(assignedTasks); }
    public ResourceVector getRemainingCapacity() { return remainingCapacity; }
    
    /**
     * Checks if a task can fit in this slot based on remaining capacity.
     * Does NOT check conflicts or SLA constraints - those are handled separately.
     * 
     * Implements feasibility constraint F2 (capacity check).
     */
    public boolean canAccommodate(Task task) {
        return task.getRequirements().fitsWithin(remainingCapacity);
    }
    
    /**
     * Assigns a task to this slot and updates remaining capacity.
     * 
     * CRITICAL: Caller must verify:
     * 1. Capacity feasibility (via canAccommodate)
     * 2. Conflict feasibility (via ConflictGraph)
     * 3. SLA feasibility (via Task.canRunInSlot)
     * 
     * This method does NOT perform validation to avoid redundant checks.
     */
    public void assignTask(Task task) {
        assignedTasks.add(task);
        remainingCapacity = remainingCapacity.subtract(task.getRequirements());
        task.setAssignedSlot(index);
    }
    
    /**
     * Removes a task from this slot and restores capacity.
     * Used during local repair and backtracking operations.
     */
    public void removeTask(Task task) {
        if (assignedTasks.remove(task)) {
            remainingCapacity = remainingCapacity.add(task.getRequirements());
            task.setAssignedSlot(null);
        }
    }
    
    /**
     * Calculates current resource utilization across all dimensions.
     * Returns array of [cpu_util, ram_util, gpu_util, network_util].
     * 
     * Used in penalty calculation for load balancing analysis.
     */
    public double[] getUtilizationRatios() {
        ResourceVector used = capacity.subtract(remainingCapacity);
        return used.utilizationRatios(capacity);
    }
    
    /**
     * Calculates maximum utilization across all dimensions.
     * Represents the bottleneck resource for this slot.
     * 
     * Example: If CPU is 90% utilized but RAM is 40%, returns 0.90.
     */
    public double getMaxUtilization() {
        ResourceVector used = capacity.subtract(remainingCapacity);
        return used.maxUtilization(capacity);
    }
    
    /**
     * Calculates GPU fragmentation penalty.
     * 
     * Design Rationale: Partial GPU allocation wastes expensive accelerator capacity.
     * If a slot has 8 GPU units and only 3 are used, the remaining 5 may be
     * insufficient for GPU-heavy tasks, leading to stranded capacity.
     * 
     * Penalty = (remaining_gpu / total_gpu) if 0 < remaining < threshold
     */
    public double getGpuFragmentationPenalty() {
        double totalGpu = capacity.getGpu();
        double remainingGpu = remainingCapacity.getGpu();
        
        if (totalGpu == 0 || remainingGpu == 0 || remainingGpu == totalGpu) {
            return 0.0;  // No fragmentation if fully used or fully empty
        }
        
        // Fragmentation occurs when GPU is partially used
        double utilizationRatio = remainingGpu / totalGpu;
        
        // Penalize moderate fragmentation (20%-80% remaining)
        if (utilizationRatio > 0.2 && utilizationRatio < 0.8) {
            return utilizationRatio * (1 - utilizationRatio) * 4;  // Peak at 50%
        }
        
        return 0.0;
    }
    
    /**
     * Calculates a fitness score for assigning a given task to this slot.
     * Higher score = better fit.
     * 
     * Scoring factors:
     * 1. Resource fit tightness (prefer slots where task uses high % of capacity)
     * 2. Avoid creating fragmentation
     * 3. Balance utilization across dimensions
     * 
     * This is the core heuristic that differentiates our algorithm from generic DSATUR.
     */
    public double calculateFitnessScore(Task task) {
        if (!canAccommodate(task)) {
            return Double.NEGATIVE_INFINITY;
        }
        
        ResourceVector taskReq = task.getRequirements();
        
        // Factor 1: Resource utilization after assignment
        ResourceVector afterRemaining = remainingCapacity.subtract(taskReq);
        double[] afterUtil = capacity.subtract(afterRemaining).utilizationRatios(capacity);
        
        // Prefer balanced utilization across dimensions (avoid bottlenecks)
        double avgUtil = 0.0;
        double variance = 0.0;
        for (double util : afterUtil) {
            avgUtil += util;
        }
        avgUtil /= ResourceVector.DIMENSIONS;
        
        for (double util : afterUtil) {
            variance += Math.pow(util - avgUtil, 2);
        }
        variance /= ResourceVector.DIMENSIONS;
        
        // Factor 2: Penalize creating GPU fragmentation
        double gpuFragPenalty = 0.0;
        if (capacity.getGpu() > 0) {
            double afterGpuRemaining = afterRemaining.getGpu();
            double gpuUtil = afterGpuRemaining / capacity.getGpu();
            if (gpuUtil > 0.2 && gpuUtil < 0.8) {
                gpuFragPenalty = 0.3;  // Moderate penalty
            }
        }
        
        // Factor 3: Prefer tighter fits (higher utilization)
        double tightnessFactor = avgUtil;
        
        // Combined score: maximize utilization, minimize variance and fragmentation
        return tightnessFactor - 0.5 * Math.sqrt(variance) - gpuFragPenalty;
    }
    
    @Override
    public String toString() {
        return String.format("Slot[%d, tasks=%d, capacity=%s, remaining=%s, util=%.2f%%]",
            index, assignedTasks.size(), capacity, remainingCapacity, 
            getMaxUtilization() * 100);
    }
}

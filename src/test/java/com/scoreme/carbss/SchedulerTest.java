package com.scoreme.carbss;

import com.scoreme.carbss.graph.ConflictGraph;
import com.scoreme.carbss.model.ResourceVector;
import com.scoreme.carbss.model.Slot;
import com.scoreme.carbss.model.Task;
import com.scoreme.carbss.scheduler.Scheduler;
import com.scoreme.carbss.validation.FeasibilityValidator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CARBSS scheduler.
 * 
 * TEST COVERAGE (as required by Task 5):
 * 1. All-conflict graph (chromatic number > K)
 * 2. Zero-capacity slot
 * 3. Tight SLA windows
 * 4. Single-task instance
 * 5. Infeasible resource instance
 * 6. Sparse conflict graph
 * 7. Heterogeneous capacities
 */
public class SchedulerTest {
    
    /**
     * Test 1: All-conflict graph (complete graph K_n where n > K).
     * 
     * Expected: Infeasible (chromatic number = n > K)
     * 
     * Design: Create 5 tasks that all conflict with each other, but only 3 slots.
     * This is a clique of size 5, requiring 5 colors, but we only have 3.
     */
    @Test
    public void testAllConflictGraph() {
        System.out.println("\n=== Test 1: All-Conflict Graph ===");
        
        ConflictGraph graph = new ConflictGraph();
        List<Slot> slots = new ArrayList<>();
        
        // Create 5 tasks
        for (int i = 0; i < 5; i++) {
            Task task = new Task(
                "T" + i,
                new ResourceVector(1, 1, 0, 0),
                0, 2,  // SLA window [0,2]
                1.0
            );
            graph.addTask(task);
        }
        
        // Add all possible conflicts (complete graph)
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                graph.addConflict("T" + i, "T" + j);
            }
        }
        
        // Create 3 slots (insufficient for 5-clique)
        for (int s = 0; s < 3; s++) {
            slots.add(new Slot(s, new ResourceVector(10, 10, 10, 10)));
        }
        
        Scheduler scheduler = new Scheduler(graph, slots);
        Scheduler.SchedulingResult result = scheduler.schedule();
        
        assertFalse(result.isFeasible(), "All-conflict graph should be infeasible");
        assertTrue(result.getViolationReason().contains("Chromatic number"),
                  "Should detect chromatic number violation");
        
        System.out.println("✓ Correctly detected infeasibility: " + result.getViolationReason());
    }
    
    /**
     * Test 2: Zero-capacity slot.
     * 
     * Expected: Infeasible (no slot can accommodate any task)
     */
    @Test
    public void testZeroCapacitySlot() {
        System.out.println("\n=== Test 2: Zero-Capacity Slot ===");
        
        ConflictGraph graph = new ConflictGraph();
        List<Slot> slots = new ArrayList<>();
        
        // Create 1 task with non-zero requirements
        Task task = new Task(
            "T0",
            new ResourceVector(4, 8, 2, 1),
            0, 0,  // Must run in slot 0
            1.0
        );
        graph.addTask(task);
        
        // Create 1 slot with zero capacity
        slots.add(new Slot(0, new ResourceVector(0, 0, 0, 0)));
        
        Scheduler scheduler = new Scheduler(graph, slots);
        Scheduler.SchedulingResult result = scheduler.schedule();
        
        assertFalse(result.isFeasible(), "Zero-capacity slot should be infeasible");
        assertTrue(result.getViolationReason().contains("Capacity"),
                  "Should detect capacity violation");
        
        System.out.println("✓ Correctly detected infeasibility: " + result.getViolationReason());
    }
    
    /**
     * Test 3: Tight SLA windows.
     * 
     * Expected: Feasible (each task has exactly one valid slot)
     * 
     * Design: Create tasks with non-overlapping single-slot windows.
     */
    @Test
    public void testTightSlaWindows() {
        System.out.println("\n=== Test 3: Tight SLA Windows ===");
        
        ConflictGraph graph = new ConflictGraph();
        List<Slot> slots = new ArrayList<>();
        
        // Create 3 tasks, each with single-slot window
        for (int i = 0; i < 3; i++) {
            Task task = new Task(
                "T" + i,
                new ResourceVector(2, 4, 1, 0.5),
                i, i,  // Window [i,i] - must run in slot i
                1.0
            );
            graph.addTask(task);
        }
        
        // Create 3 slots with sufficient capacity
        for (int s = 0; s < 3; s++) {
            slots.add(new Slot(s, new ResourceVector(10, 20, 5, 3)));
        }
        
        Scheduler scheduler = new Scheduler(graph, slots);
        Scheduler.SchedulingResult result = scheduler.schedule();
        
        assertTrue(result.isFeasible(), "Tight SLA windows should be feasible");
        
        // Validate assignment
        FeasibilityValidator.ValidationResult validation = 
            FeasibilityValidator.validate(graph, slots);
        assertTrue(validation.isFeasible(), "Assignment should satisfy all constraints");
        
        System.out.println("✓ Successfully scheduled with tight SLA windows");
        System.out.println("  Penalty: " + result.getPenalty());
    }
    
    /**
     * Test 4: Single-task instance.
     * 
     * Expected: Feasible (trivial case)
     */
    @Test
    public void testSingleTask() {
        System.out.println("\n=== Test 4: Single Task ===");
        
        ConflictGraph graph = new ConflictGraph();
        List<Slot> slots = new ArrayList<>();
        
        Task task = new Task(
            "T0",
            new ResourceVector(4, 8, 2, 1),
            0, 2,
            5.0
        );
        graph.addTask(task);
        
        for (int s = 0; s < 3; s++) {
            slots.add(new Slot(s, new ResourceVector(10, 20, 5, 3)));
        }
        
        Scheduler scheduler = new Scheduler(graph, slots);
        Scheduler.SchedulingResult result = scheduler.schedule();
        
        assertTrue(result.isFeasible(), "Single task should be feasible");
        assertTrue(task.isAssigned(), "Task should be assigned");
        assertEquals(0, task.getAssignedSlot(), "Should assign to earliest slot");
        
        System.out.println("✓ Single task assigned to slot " + task.getAssignedSlot());
    }
    
    /**
     * Test 5: Infeasible resource instance.
     * 
     * Expected: Infeasible (total resource demand exceeds total capacity)
     */
    @Test
    public void testInfeasibleResources() {
        System.out.println("\n=== Test 5: Infeasible Resources ===");
        
        ConflictGraph graph = new ConflictGraph();
        List<Slot> slots = new ArrayList<>();
        
        // Create 3 tasks, each requiring 10 GPU units
        for (int i = 0; i < 3; i++) {
            Task task = new Task(
                "T" + i,
                new ResourceVector(1, 1, 10, 0),  // 10 GPUs each
                0, 2,
                1.0
            );
            graph.addTask(task);
        }
        
        // Create 3 slots, each with only 8 GPU units
        // Total demand = 30 GPUs, total capacity = 24 GPUs
        for (int s = 0; s < 3; s++) {
            slots.add(new Slot(s, new ResourceVector(100, 100, 8, 100)));
        }
        
        Scheduler scheduler = new Scheduler(graph, slots);
        Scheduler.SchedulingResult result = scheduler.schedule();
        
        assertFalse(result.isFeasible(), "Insufficient resources should be infeasible");
        
        System.out.println("✓ Correctly detected resource infeasibility");
    }
    
    /**
     * Test 6: Sparse conflict graph.
     * 
     * Expected: Feasible (low conflict density allows easy scheduling)
     */
    @Test
    public void testSparseConflictGraph() {
        System.out.println("\n=== Test 6: Sparse Conflict Graph ===");
        
        ConflictGraph graph = new ConflictGraph();
        List<Slot> slots = new ArrayList<>();
        
        // Create 10 tasks
        for (int i = 0; i < 10; i++) {
            Task task = new Task(
                "T" + i,
                new ResourceVector(2, 4, 1, 0.5),
                0, 4,
                1.0 + i * 0.5
            );
            graph.addTask(task);
        }
        
        // Add only 3 conflicts (very sparse)
        graph.addConflict("T0", "T1");
        graph.addConflict("T2", "T3");
        graph.addConflict("T4", "T5");
        
        // Create 5 slots
        for (int s = 0; s < 5; s++) {
            slots.add(new Slot(s, new ResourceVector(10, 20, 5, 3)));
        }
        
        Scheduler scheduler = new Scheduler(graph, slots);
        Scheduler.SchedulingResult result = scheduler.schedule();
        
        assertTrue(result.isFeasible(), "Sparse conflict graph should be feasible");
        
        FeasibilityValidator.ValidationResult validation = 
            FeasibilityValidator.validate(graph, slots);
        assertTrue(validation.isFeasible(), "Assignment should be valid");
        
        System.out.println("✓ Successfully scheduled sparse graph");
        System.out.println("  Penalty: " + result.getPenalty());
    }
    
    /**
     * Test 7: Heterogeneous slot capacities.
     * 
     * Expected: Feasible (scheduler should adapt to varying capacities)
     */
    @Test
    public void testHeterogeneousCapacities() {
        System.out.println("\n=== Test 7: Heterogeneous Capacities ===");
        
        ConflictGraph graph = new ConflictGraph();
        List<Slot> slots = new ArrayList<>();
        
        // Create 3 tasks with different resource profiles
        Task cpuHeavy = new Task("T0", new ResourceVector(16, 8, 0, 1), 0, 2, 3.0);
        Task gpuHeavy = new Task("T1", new ResourceVector(4, 8, 6, 1), 0, 2, 5.0);
        Task balanced = new Task("T2", new ResourceVector(8, 16, 2, 2), 0, 2, 2.0);
        
        graph.addTask(cpuHeavy);
        graph.addTask(gpuHeavy);
        graph.addTask(balanced);
        
        // Create 3 slots with heterogeneous capacities
        slots.add(new Slot(0, new ResourceVector(32, 64, 2, 4)));   // CPU-heavy slot
        slots.add(new Slot(1, new ResourceVector(16, 32, 8, 4)));   // GPU-heavy slot
        slots.add(new Slot(2, new ResourceVector(20, 40, 4, 6)));   // Balanced slot
        
        Scheduler scheduler = new Scheduler(graph, slots);
        Scheduler.SchedulingResult result = scheduler.schedule();
        
        assertTrue(result.isFeasible(), "Heterogeneous capacities should be feasible");
        
        FeasibilityValidator.ValidationResult validation = 
            FeasibilityValidator.validate(graph, slots);
        assertTrue(validation.isFeasible(), "Assignment should be valid");
        
        System.out.println("✓ Successfully scheduled with heterogeneous capacities");
        System.out.println("  T0 (CPU-heavy) → Slot " + cpuHeavy.getAssignedSlot());
        System.out.println("  T1 (GPU-heavy) → Slot " + gpuHeavy.getAssignedSlot());
        System.out.println("  T2 (Balanced)  → Slot " + balanced.getAssignedSlot());
    }
    
    /**
     * Test 8: Toy instance from assignment specification.
     */
    @Test
    public void testToyInstance() {
        System.out.println("\n=== Test 8: Assignment Toy Instance ===");
        
        ConflictGraph graph = new ConflictGraph();
        List<Slot> slots = new ArrayList<>();
        
        // Create tasks from specification
        Task t1 = new Task("T1", new ResourceVector(8, 32, 4, 1.5), 0, 2, 5.0);
        Task t2 = new Task("T2", new ResourceVector(4, 16, 0, 3.0), 0, 3, 4.0);
        Task t3 = new Task("T3", new ResourceVector(2, 8, 0, 2.0), 0, 3, 3.0);
        Task t4 = new Task("T4", new ResourceVector(16, 64, 2, 0.5), 1, 3, 2.0);
        Task t5 = new Task("T5", new ResourceVector(8, 32, 2, 1.0), 0, 3, 3.0);
        Task t6 = new Task("T6", new ResourceVector(4, 16, 0, 1.5), 1, 3, 2.0);
        
        graph.addTask(t1);
        graph.addTask(t2);
        graph.addTask(t3);
        graph.addTask(t4);
        graph.addTask(t5);
        graph.addTask(t6);
        
        // Add conflicts from specification
        graph.addConflict("T1", "T2");
        graph.addConflict("T1", "T3");
        graph.addConflict("T2", "T4");
        graph.addConflict("T3", "T5");
        graph.addConflict("T4", "T6");
        graph.addConflict("T5", "T6");
        
        // Create 4 slots with uniform capacity
        for (int s = 0; s < 4; s++) {
            slots.add(new Slot(s, new ResourceVector(32, 128, 8, 6.0)));
        }
        
        Scheduler scheduler = new Scheduler(graph, slots);
        Scheduler.SchedulingResult result = scheduler.schedule();
        
        assertTrue(result.isFeasible(), "Toy instance should be feasible");
        
        FeasibilityValidator.ValidationResult validation = 
            FeasibilityValidator.validate(graph, slots);
        assertTrue(validation.isFeasible(), "Assignment should be valid");
        
        System.out.println("✓ Successfully scheduled toy instance");
        System.out.println("  Penalty: " + result.getPenalty());
        System.out.println("  Runtime: " + result.getRuntimeMs() + " ms");
    }
}

package com.scoreme.carbss;

import com.scoreme.carbss.graph.ConflictGraph;
import com.scoreme.carbss.io.InstanceParser;
import com.scoreme.carbss.io.ResultWriter;
import com.scoreme.carbss.model.Slot;
import com.scoreme.carbss.penalty.PenaltyCalculator;
import com.scoreme.carbss.scheduler.Scheduler;
import com.scoreme.carbss.validation.FeasibilityValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for CARBSS scheduler.
 * 
 * Usage:
 *   java -jar carbss.jar <input.json> [output.json]
 * 
 * If output path is not specified, results are printed to console.
 */
public class Main {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar carbss.jar <input.json> [output.json]");
            System.exit(1);
        }
        
        String inputPath = args[0];
        String outputPath = args.length > 1 ? args[1] : null;
        
        try {
            System.out.println("=== CARBSS: Conflict-Aware Resource-Bounded Slot Scheduler ===");
            System.out.println("Input: " + inputPath);
            System.out.println();
            
            // Parse instance
            System.out.println("Parsing instance...");
            InstanceParser parser = new InstanceParser();
            InstanceParser.ProblemInstance instance = parser.parseInstance(inputPath);
            
            ConflictGraph conflictGraph = instance.getConflictGraph();
            List<Slot> slots = instance.getSlots();
            
            System.out.println("Instance loaded:");
            System.out.println("  Tasks: " + conflictGraph.getTaskCount());
            System.out.println("  Slots: " + slots.size());
            System.out.println("  Conflicts: " + conflictGraph.getEdgeCount());
            System.out.println("  Conflict density: " + String.format("%.3f", 
                conflictGraph.getTaskCount() > 1 
                    ? (2.0 * conflictGraph.getEdgeCount()) / 
                      (conflictGraph.getTaskCount() * (conflictGraph.getTaskCount() - 1))
                    : 0.0));
            System.out.println();
            
            // Run scheduler
            System.out.println("Running PW-DSATUR-RF scheduler...");
            Scheduler scheduler = new Scheduler(conflictGraph, slots);
            Scheduler.SchedulingResult result = scheduler.schedule();
            
            System.out.println("Scheduling completed in " + result.getRuntimeMs() + " ms");
            System.out.println();
            
            // Validate result
            if (result.isFeasible()) {
                System.out.println("Validating feasibility...");
                FeasibilityValidator.ValidationResult validation = 
                    FeasibilityValidator.validate(conflictGraph, slots);
                
                if (!validation.isFeasible()) {
                    System.err.println("ERROR: Scheduler produced infeasible solution!");
                    for (String violation : validation.getViolations()) {
                        System.err.println("  " + violation);
                    }
                    System.exit(1);
                }
                
                System.out.println("✓ All feasibility constraints satisfied");
                System.out.println();
                
                // Calculate and display penalty breakdown
                PenaltyCalculator.PenaltyBreakdown breakdown = 
                    PenaltyCalculator.calculateBreakdown(
                        new ArrayList<>(conflictGraph.getAllTasks()), slots);
                
                System.out.println("Penalty Breakdown:");
                System.out.println("  Base delay:        " + String.format("%.2f", breakdown.baseDelay));
                System.out.println("  SLA risk:          " + String.format("%.2f", breakdown.slaRisk));
                System.out.println("  Load imbalance:    " + String.format("%.2f", breakdown.loadImbalance));
                System.out.println("  GPU fragmentation: " + String.format("%.2f", breakdown.gpuFragmentation));
                System.out.println("  ─────────────────────────");
                System.out.println("  TOTAL PENALTY:     " + String.format("%.2f", breakdown.total));
                System.out.println();
                
                // Display slot utilization
                System.out.println("Slot Utilization:");
                for (Slot slot : slots) {
                    System.out.println(String.format("  Slot %d: %.1f%% (tasks=%d)",
                        slot.getIndex(), slot.getMaxUtilization() * 100, 
                        slot.getAssignedTasks().size()));
                }
                System.out.println();
            } else {
                System.out.println("✗ Infeasible: " + result.getViolationReason());
                System.out.println();
            }
            
            // Write output
            ResultWriter writer = new ResultWriter();
            if (outputPath != null) {
                writer.writeResult(result, conflictGraph, outputPath);
                System.out.println("Results written to: " + outputPath);
            } else {
                System.out.println("JSON Output:");
                System.out.println(writer.resultToJson(result, conflictGraph));
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

package com.scoreme.carbss.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scoreme.carbss.graph.ConflictGraph;
import com.scoreme.carbss.model.Task;
import com.scoreme.carbss.scheduler.Scheduler.SchedulingResult;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes scheduling results to JSON output file.
 * 
 * OUTPUT FORMAT (as specified in assignment):
 * {
 *   "assignment": {"T0": 1, "T1": 2, ...},
 *   "penalty": 123.45,
 *   "runtime_ms": 456,
 *   "feasible": true,
 *   "violation_reason": null
 * }
 */
public class ResultWriter {
    
    private final ObjectMapper mapper;
    
    public ResultWriter() {
        this.mapper = new ObjectMapper();
    }
    
    /**
     * Writes scheduling result to JSON file.
     */
    public void writeResult(SchedulingResult result, ConflictGraph conflictGraph, 
                           String outputPath) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        
        // Write assignment map
        ObjectNode assignmentNode = mapper.createObjectNode();
        if (result.isFeasible()) {
            for (Task task : conflictGraph.getAllTasks()) {
                if (task.isAssigned()) {
                    assignmentNode.put(task.getId(), task.getAssignedSlot());
                }
            }
        }
        root.set("assignment", assignmentNode);
        
        // Write penalty
        if (result.getPenalty() != null) {
            root.put("penalty", result.getPenalty());
        } else {
            root.putNull("penalty");
        }
        
        // Write runtime
        root.put("runtime_ms", result.getRuntimeMs());
        
        // Write feasibility
        root.put("feasible", result.isFeasible());
        
        // Write violation reason
        if (result.getViolationReason() != null) {
            root.put("violation_reason", result.getViolationReason());
        } else {
            root.putNull("violation_reason");
        }
        
        // Write to file
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), root);
    }
    
    /**
     * Converts result to JSON string for console output.
     */
    public String resultToJson(SchedulingResult result, ConflictGraph conflictGraph) {
        try {
            ObjectNode root = mapper.createObjectNode();
            
            ObjectNode assignmentNode = mapper.createObjectNode();
            if (result.isFeasible()) {
                for (Task task : conflictGraph.getAllTasks()) {
                    if (task.isAssigned()) {
                        assignmentNode.put(task.getId(), task.getAssignedSlot());
                    }
                }
            }
            root.set("assignment", assignmentNode);
            
            if (result.getPenalty() != null) {
                root.put("penalty", result.getPenalty());
            } else {
                root.putNull("penalty");
            }
            
            root.put("runtime_ms", result.getRuntimeMs());
            root.put("feasible", result.isFeasible());
            
            if (result.getViolationReason() != null) {
                root.put("violation_reason", result.getViolationReason());
            } else {
                root.putNull("violation_reason");
            }
            
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}

package com.scoreme.carbss.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scoreme.carbss.graph.ConflictGraph;
import com.scoreme.carbss.model.ResourceVector;
import com.scoreme.carbss.model.Slot;
import com.scoreme.carbss.model.Task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses JSON instance files into internal data structures.
 * 
 * EXPECTED JSON FORMAT (from instance generator):
 * {
 *   "tasks": ["T0", "T1", ...],
 *   "conflicts": [[0,1], [0,2], ...],
 *   "resources": [[cpu, ram, gpu, net], ...],
 *   "capacities": [[cpu, ram, gpu, net], ...],
 *   "windows": [[lower, upper], ...],
 *   "weights": [w0, w1, ...],
 *   "K": number_of_slots
 * }
 */
public class InstanceParser {
    
    private final ObjectMapper mapper;
    
    public InstanceParser() {
        this.mapper = new ObjectMapper();
    }
    
    /**
     * Parses a JSON instance file and returns a ProblemInstance.
     */
    public ProblemInstance parseInstance(String filePath) throws IOException {
        File file = new File(filePath);
        JsonNode root = mapper.readTree(file);
        
        // Parse basic parameters
        int K = root.get("K").asInt();
        
        // Parse tasks
        JsonNode tasksNode = root.get("tasks");
        JsonNode resourcesNode = root.get("resources");
        JsonNode windowsNode = root.get("windows");
        JsonNode weightsNode = root.get("weights");
        
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < tasksNode.size(); i++) {
            String taskId = tasksNode.get(i).asText();
            
            JsonNode resourceArray = resourcesNode.get(i);
            ResourceVector requirements = new ResourceVector(
                resourceArray.get(0).asDouble(),
                resourceArray.get(1).asDouble(),
                resourceArray.get(2).asDouble(),
                resourceArray.get(3).asDouble()
            );
            
            JsonNode windowArray = windowsNode.get(i);
            int lowerBound = windowArray.get(0).asInt();
            int upperBound = windowArray.get(1).asInt();
            
            double weight = weightsNode.get(i).asDouble();
            
            Task task = new Task(taskId, requirements, lowerBound, upperBound, weight);
            tasks.add(task);
        }
        
        // Parse conflict graph
        ConflictGraph conflictGraph = new ConflictGraph();
        for (Task task : tasks) {
            conflictGraph.addTask(task);
        }
        
        JsonNode conflictsNode = root.get("conflicts");
        for (JsonNode conflictPair : conflictsNode) {
            int i = conflictPair.get(0).asInt();
            int j = conflictPair.get(1).asInt();
            conflictGraph.addConflict(tasks.get(i).getId(), tasks.get(j).getId());
        }
        
        // Parse slots
        JsonNode capacitiesNode = root.get("capacities");
        List<Slot> slots = new ArrayList<>();
        for (int s = 0; s < K; s++) {
            JsonNode capacityArray = capacitiesNode.get(s);
            ResourceVector capacity = new ResourceVector(
                capacityArray.get(0).asDouble(),
                capacityArray.get(1).asDouble(),
                capacityArray.get(2).asDouble(),
                capacityArray.get(3).asDouble()
            );
            slots.add(new Slot(s, capacity));
        }
        
        return new ProblemInstance(conflictGraph, slots, K);
    }
    
    /**
     * Container for parsed problem instance.
     */
    public static class ProblemInstance {
        private final ConflictGraph conflictGraph;
        private final List<Slot> slots;
        private final int K;
        
        public ProblemInstance(ConflictGraph conflictGraph, List<Slot> slots, int K) {
            this.conflictGraph = conflictGraph;
            this.slots = slots;
            this.K = K;
        }
        
        public ConflictGraph getConflictGraph() { return conflictGraph; }
        public List<Slot> getSlots() { return slots; }
        public int getK() { return K; }
    }
}

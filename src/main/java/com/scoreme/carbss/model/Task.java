package com.scoreme.carbss.model;

/**
 * Represents a credit pipeline task with resource requirements, SLA constraints,
 * and business priority weight.
 * 
 * Design Rationale: Each task encapsulates all scheduling-relevant metadata.
 * The SLA window [lowerBound, upperBound] enforces temporal feasibility (F3).
 * Priority weight reflects lender tier importance (PSU banks > NBFCs).
 */
public class Task {
    private final String id;
    private final ResourceVector requirements;
    private final int slaLowerBound;  // Earliest permissible slot
    private final int slaUpperBound;  // Latest permissible slot
    private final double priorityWeight;
    
    // Scheduling state (mutable during algorithm execution)
    private Integer assignedSlot;
    
    public Task(String id, ResourceVector requirements, 
                int slaLowerBound, int slaUpperBound, double priorityWeight) {
        if (slaLowerBound < 0 || slaUpperBound < slaLowerBound) {
            throw new IllegalArgumentException(
                "Invalid SLA window: [" + slaLowerBound + ", " + slaUpperBound + "]");
        }
        
        this.id = id;
        this.requirements = requirements;
        this.slaLowerBound = slaLowerBound;
        this.slaUpperBound = slaUpperBound;
        this.priorityWeight = priorityWeight;
        this.assignedSlot = null;
    }
    
    public String getId() { return id; }
    public ResourceVector getRequirements() { return requirements; }
    public int getSlaLowerBound() { return slaLowerBound; }
    public int getSlaUpperBound() { return slaUpperBound; }
    public double getPriorityWeight() { return priorityWeight; }
    
    public Integer getAssignedSlot() { return assignedSlot; }
    public void setAssignedSlot(Integer slot) { this.assignedSlot = slot; }
    public boolean isAssigned() { return assignedSlot != null; }
    
    /**
     * Checks if a given slot index falls within this task's SLA window.
     * Critical for feasibility constraint F3.
     */
    public boolean canRunInSlot(int slotIndex) {
        return slotIndex >= slaLowerBound && slotIndex <= slaUpperBound;
    }
    
    /**
     * Calculates SLA urgency score based on window tightness.
     * Tasks with narrow windows (e.g., [1,2]) are more urgent than wide windows (e.g., [1,10]).
     * 
     * Design Decision: Urgency = 1 / (window_width + 1)
     * This prioritizes tasks that have fewer scheduling options.
     */
    public double calculateUrgency() {
        int windowWidth = slaUpperBound - slaLowerBound + 1;
        return 1.0 / windowWidth;
    }
    
    /**
     * Calculates SLA breach risk for a given slot assignment.
     * Tasks assigned near their upper bound carry higher operational risk.
     * 
     * Risk increases exponentially as we approach the deadline:
     * - Slot at lower bound: risk = 0.0
     * - Slot at upper bound: risk = 1.0
     */
    public double calculateSlaRisk(int slotIndex) {
        if (!canRunInSlot(slotIndex)) {
            return Double.POSITIVE_INFINITY;  // Infeasible assignment
        }
        
        int windowWidth = slaUpperBound - slaLowerBound;
        if (windowWidth == 0) {
            return 0.0;  // Single-slot window has no flexibility
        }
        
        double position = (double)(slotIndex - slaLowerBound) / windowWidth;
        return Math.pow(position, 2);  // Quadratic risk growth
    }
    
    @Override
    public String toString() {
        return String.format("Task[id=%s, req=%s, sla=[%d,%d], weight=%.2f, assigned=%s]",
            id, requirements, slaLowerBound, slaUpperBound, priorityWeight, assignedSlot);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return id.equals(task.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

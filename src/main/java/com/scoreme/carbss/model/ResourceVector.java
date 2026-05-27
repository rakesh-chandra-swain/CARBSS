package com.scoreme.carbss.model;

import java.util.Arrays;

/**
 * Represents a 4-dimensional resource requirement or capacity vector.
 * Dimensions: [CPU_cores, RAM_GB, GPU_units, Network_Gbps]
 * 
 * Design Decision: Immutable value object to prevent accidental mutation
 * during scheduling operations, ensuring thread-safety for parallel extensions.
 */
public class ResourceVector {
    private final double[] resources;
    
    public static final int CPU_INDEX = 0;
    public static final int RAM_INDEX = 1;
    public static final int GPU_INDEX = 2;
    public static final int NETWORK_INDEX = 3;
    public static final int DIMENSIONS = 4;
    
    public ResourceVector(double cpu, double ram, double gpu, double network) {
        this.resources = new double[]{cpu, ram, gpu, network};
    }
    
    public ResourceVector(double[] resources) {
        if (resources.length != DIMENSIONS) {
            throw new IllegalArgumentException("Resource vector must have exactly 4 dimensions");
        }
        this.resources = Arrays.copyOf(resources, DIMENSIONS);
    }
    
    public double getCpu() { return resources[CPU_INDEX]; }
    public double getRam() { return resources[RAM_INDEX]; }
    public double getGpu() { return resources[GPU_INDEX]; }
    public double getNetwork() { return resources[NETWORK_INDEX]; }
    
    public double get(int dimension) {
        if (dimension < 0 || dimension >= DIMENSIONS) {
            throw new IndexOutOfBoundsException("Invalid dimension: " + dimension);
        }
        return resources[dimension];
    }
    
    /**
     * Checks if this vector fits within the capacity vector.
     * Critical for feasibility constraint F2.
     */
    public boolean fitsWithin(ResourceVector capacity) {
        for (int d = 0; d < DIMENSIONS; d++) {
            if (this.resources[d] > capacity.resources[d]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Component-wise addition for resource aggregation.
     * Used during slot capacity tracking.
     */
    public ResourceVector add(ResourceVector other) {
        double[] result = new double[DIMENSIONS];
        for (int d = 0; d < DIMENSIONS; d++) {
            result[d] = this.resources[d] + other.resources[d];
        }
        return new ResourceVector(result);
    }
    
    /**
     * Component-wise subtraction for resource deallocation.
     */
    public ResourceVector subtract(ResourceVector other) {
        double[] result = new double[DIMENSIONS];
        for (int d = 0; d < DIMENSIONS; d++) {
            result[d] = this.resources[d] - other.resources[d];
        }
        return new ResourceVector(result);
    }
    
    /**
     * Calculates utilization ratio for each dimension.
     * Used in penalty calculation for resource wastage analysis.
     */
    public double[] utilizationRatios(ResourceVector capacity) {
        double[] ratios = new double[DIMENSIONS];
        for (int d = 0; d < DIMENSIONS; d++) {
            ratios[d] = capacity.resources[d] > 0 
                ? this.resources[d] / capacity.resources[d] 
                : 0.0;
        }
        return ratios;
    }
    
    /**
     * Calculates maximum utilization across all dimensions.
     * Represents the bottleneck resource.
     */
    public double maxUtilization(ResourceVector capacity) {
        double max = 0.0;
        for (int d = 0; d < DIMENSIONS; d++) {
            if (capacity.resources[d] > 0) {
                max = Math.max(max, this.resources[d] / capacity.resources[d]);
            }
        }
        return max;
    }
    
    @Override
    public String toString() {
        return String.format("[CPU:%.2f, RAM:%.2f, GPU:%.2f, Net:%.2f]",
            resources[CPU_INDEX], resources[RAM_INDEX], 
            resources[GPU_INDEX], resources[NETWORK_INDEX]);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceVector)) return false;
        ResourceVector that = (ResourceVector) o;
        return Arrays.equals(resources, that.resources);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(resources);
    }
}

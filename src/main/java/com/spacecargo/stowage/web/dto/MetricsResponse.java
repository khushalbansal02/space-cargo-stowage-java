package com.spacecargo.stowage.web.dto;

import java.util.List;
import java.util.Map;

/**
 * Operational analytics for the whole station: how full the containers are, how
 * accessible the stowed items are on average, and how much waste is outstanding.
 */
public record MetricsResponse(
        int containerCount,
        long itemCount,
        Map<String, Long> itemsByStatus,
        double totalCapacity,
        double occupiedVolume,
        double utilizationPercent,
        double averageRetrievalSteps,
        int wasteItemCount,
        double wasteMass,
        List<ContainerUtilization> containers) {

    /** Per-container fill level. */
    public record ContainerUtilization(
            String containerId,
            String zone,
            double capacity,
            double occupiedVolume,
            double utilizationPercent,
            int itemCount) {
    }
}

package com.spacecargo.stowage.web.dto;

import com.spacecargo.stowage.domain.Item;

import java.time.LocalDate;

/** Item view returned to clients, including its placement when stowed. */
public record ItemResponse(
        String itemId,
        String name,
        double width,
        double depth,
        double height,
        Double mass,
        int priority,
        String status,
        LocalDate expiryDate,
        Integer usageLimit,
        Integer remainingUses,
        String preferredZone,
        String preferredContainerId,
        PlacementView placement) {

    public static ItemResponse from(Item i) {
        return new ItemResponse(
                i.getItemId(), i.getName(),
                i.getDimensionW(), i.getDimensionD(), i.getDimensionH(),
                i.getMass(), i.getPriority(), i.getStatus().name(),
                i.getExpiryDate(), i.getUsageLimit(), i.getRemainingUses(),
                i.getPreferredZone(), i.getPreferredContainerId(),
                PlacementView.from(i));
    }
}

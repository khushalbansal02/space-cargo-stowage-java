package com.spacecargo.stowage.placement;

import com.spacecargo.stowage.domain.geometry.BoundingBox;

/**
 * Result of attempting to place one item.
 *
 * @param itemId      the item the engine tried to place
 * @param placed      whether a spot was found
 * @param containerId container it was placed in (null when not placed)
 * @param box         where it was placed (null when not placed)
 * @param preferred   whether the chosen container satisfied the item's zone/container preference
 * @param reason      human-readable explanation, primarily for failures
 */
public record PlacementOutcome(
        String itemId,
        boolean placed,
        String containerId,
        BoundingBox box,
        boolean preferred,
        String reason) {

    public static PlacementOutcome placed(String itemId, String containerId, BoundingBox box, boolean preferred) {
        String reason = preferred
                ? "Placed in preferred container/zone."
                : "Placed in best available container (outside preference).";
        return new PlacementOutcome(itemId, true, containerId, box, preferred, reason);
    }

    public static PlacementOutcome failed(String itemId, String reason) {
        return new PlacementOutcome(itemId, false, null, null, false, reason);
    }
}

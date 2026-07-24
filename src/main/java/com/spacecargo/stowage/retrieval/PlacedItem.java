package com.spacecargo.stowage.retrieval;

import com.spacecargo.stowage.domain.geometry.BoundingBox;

/** An item together with where it sits, as consumed by the retrieval planner. */
public record PlacedItem(String itemId, String name, BoundingBox box) {
}

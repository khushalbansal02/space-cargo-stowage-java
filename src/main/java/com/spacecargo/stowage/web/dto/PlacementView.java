package com.spacecargo.stowage.web.dto;

import com.spacecargo.stowage.domain.Item;
import com.spacecargo.stowage.domain.geometry.BoundingBox;

/** The location of a stowed item, or null fields when it is not placed. */
public record PlacementView(
        String containerId,
        double x, double y, double z,
        double width, double depth, double height) {

    public static PlacementView from(Item item) {
        if (!item.isStowed()) {
            return null;
        }
        BoundingBox b = item.stowedBox();
        return new PlacementView(item.getContainerId(), b.x(), b.y(), b.z(), b.w(), b.d(), b.h());
    }

    public static PlacementView from(String containerId, BoundingBox b) {
        return new PlacementView(containerId, b.x(), b.y(), b.z(), b.w(), b.d(), b.h());
    }
}

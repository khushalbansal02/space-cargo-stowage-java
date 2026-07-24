package com.spacecargo.stowage.web.dto;

import java.util.List;

/**
 * Request to run the placement engine.
 *
 * @param itemIds items to place; when null/empty, every AVAILABLE item is placed
 * @param userId  actor recorded in the audit log
 */
public record PlacementRequest(List<String> itemIds, String userId) {

    public String userIdOrDefault() {
        return (userId == null || userId.isBlank()) ? "system" : userId;
    }
}

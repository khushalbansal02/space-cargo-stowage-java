package com.spacecargo.stowage.web.dto;

/** Body for confirming a crew retrieval. */
public record RetrieveRequest(String userId) {

    public String userIdOrDefault() {
        return (userId == null || userId.isBlank()) ? "system" : userId;
    }
}

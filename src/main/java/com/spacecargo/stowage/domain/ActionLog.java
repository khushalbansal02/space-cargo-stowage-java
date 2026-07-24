package com.spacecargo.stowage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Append-only audit record of a mutating action (placement, move, retrieval,
 * simulation, disposal, import). Powers the {@code /api/v1/logs} endpoint.
 */
@Entity
@Table(name = "action_logs")
public class ActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "user_id")
    private String userId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "item_id")
    private String itemId;

    @Column(name = "details_json")
    private String detailsJson;

    protected ActionLog() {
    }

    public ActionLog(String userId, String actionType, String itemId, String detailsJson) {
        this.timestamp = Instant.now();
        this.userId = userId;
        this.actionType = actionType;
        this.itemId = itemId;
        this.detailsJson = detailsJson;
    }

    public Long getLogId() { return logId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
}

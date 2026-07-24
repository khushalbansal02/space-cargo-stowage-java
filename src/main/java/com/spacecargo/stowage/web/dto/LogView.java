package com.spacecargo.stowage.web.dto;

import com.spacecargo.stowage.domain.ActionLog;

import java.time.Instant;

/** A single audit-log entry as returned to clients. */
public record LogView(
        Long logId,
        Instant timestamp,
        String userId,
        String actionType,
        String itemId,
        String detailsJson) {

    public static LogView from(ActionLog log) {
        return new LogView(log.getLogId(), log.getTimestamp(), log.getUserId(),
                log.getActionType(), log.getItemId(), log.getDetailsJson());
    }
}

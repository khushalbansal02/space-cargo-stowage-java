package com.spacecargo.stowage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spacecargo.stowage.domain.ActionLog;
import com.spacecargo.stowage.repository.ActionLogRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Writes append-only audit records. Every mutating action (placement, move,
 * retrieval, simulation, disposal, import) records who did what, to which item,
 * with a JSON detail blob.
 */
@Service
public class AuditService {

    private final ActionLogRepository logs;
    private final ObjectMapper objectMapper;

    public AuditService(ActionLogRepository logs, ObjectMapper objectMapper) {
        this.logs = logs;
        this.objectMapper = objectMapper;
    }

    public void record(String userId, String actionType, String itemId, Map<String, ?> details) {
        logs.save(new ActionLog(userId, actionType, itemId, toJson(details)));
    }

    private String toJson(Map<String, ?> details) {
        if (details == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            return "{\"_error\":\"could not serialise details\"}";
        }
    }
}

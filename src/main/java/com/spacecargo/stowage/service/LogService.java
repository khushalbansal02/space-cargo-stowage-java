package com.spacecargo.stowage.service;

import com.spacecargo.stowage.domain.ActionLog;
import com.spacecargo.stowage.repository.ActionLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to the audit trail, optionally filtered by action type or item.
 */
@Service
public class LogService {

    private final ActionLogRepository logs;

    public LogService(ActionLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public Page<ActionLog> find(String actionType, String itemId, Pageable pageable) {
        if (actionType != null && !actionType.isBlank()) {
            return logs.findByActionType(actionType, pageable);
        }
        if (itemId != null && !itemId.isBlank()) {
            return logs.findByItemId(itemId, pageable);
        }
        return logs.findAll(pageable);
    }
}

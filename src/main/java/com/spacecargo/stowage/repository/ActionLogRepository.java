package com.spacecargo.stowage.repository;

import com.spacecargo.stowage.domain.ActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {

    Page<ActionLog> findByActionType(String actionType, Pageable pageable);

    Page<ActionLog> findByItemId(String itemId, Pageable pageable);
}

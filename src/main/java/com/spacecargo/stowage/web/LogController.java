package com.spacecargo.stowage.web;

import com.spacecargo.stowage.service.LogService;
import com.spacecargo.stowage.web.dto.LogView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {

    private final LogService logs;

    public LogController(LogService logs) {
        this.logs = logs;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<LogView> result = logs.find(actionType, itemId, pageable).map(LogView::from);

        List<LogView> content = result.getContent();
        return Map.of(
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "logs", content);
    }
}

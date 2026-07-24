package com.spacecargo.stowage.web;

import com.spacecargo.stowage.service.AnalyticsService;
import com.spacecargo.stowage.web.dto.MetricsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final AnalyticsService analytics;

    public MetricsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping
    public MetricsResponse metrics() {
        return analytics.computeMetrics();
    }
}

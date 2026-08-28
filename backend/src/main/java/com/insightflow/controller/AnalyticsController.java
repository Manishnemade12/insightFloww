package com.insightflow.controller;

import com.insightflow.model.AnalyticsSnapshot;
import com.insightflow.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsSnapshot> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getMetrics());
    }
}

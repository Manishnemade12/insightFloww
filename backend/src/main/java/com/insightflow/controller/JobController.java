package com.insightflow.controller;

import com.insightflow.model.JobRecord;
import com.insightflow.model.SubmitRequest;
import com.insightflow.model.SubmitResponse;
import com.insightflow.model.SummaryPayload;
import com.insightflow.service.AnalyticsService;
import com.insightflow.service.ProcessorService;
import com.insightflow.service.RedisStoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class JobController {

    private final RedisStoreService store;
    private final ProcessorService processorService;
    private final AnalyticsService analytics;

    public JobController(RedisStoreService store, ProcessorService processorService, AnalyticsService analytics) {
        this.store = store;
        this.processorService = processorService;
        this.analytics = analytics;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitJob(@Valid @RequestBody SubmitRequest request) {
        analytics.trackRequest();

        String input = request.getInput() != null ? request.getInput().trim() : "";
        if (input.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "input field is required"));
        }

        String inputHash = processorService.getInputHash(input);
        SummaryPayload cachedSummary = store.getSummary(inputHash);

        if (cachedSummary != null) {
            analytics.trackCacheHit();
            SubmitResponse response = new SubmitResponse(
                    inputHash,
                    "completed",
                    true,
                    cachedSummary.getSummary(),
                    cachedSummary.getTags()
            );
            return ResponseEntity.ok(response);
        }

        analytics.trackCacheMiss();

        String jobId = UUID.randomUUID().toString();
        JobRecord job = processorService.createJob(jobId, input);

        store.setJob(job);
        store.pushQueue(jobId);

        SubmitResponse response = new SubmitResponse(jobId, "pending", false);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        String cleanJobId = jobId != null ? jobId.trim() : "";
        if (cleanJobId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "job_id is required"));
        }

        JobRecord job = store.getJob(cleanJobId);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "job not found"));
        }

        return ResponseEntity.ok(job);
    }
}

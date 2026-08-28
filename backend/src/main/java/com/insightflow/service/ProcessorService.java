package com.insightflow.service;

import com.insightflow.model.JobRecord;
import com.insightflow.model.JobStatus;
import com.insightflow.model.SummaryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

@Service
public class ProcessorService {

    private static final Logger log = LoggerFactory.getLogger(ProcessorService.class);

    private final RedisStoreService store;
    private final AnalyticsService analytics;
    private final GeminiAiService aiService;
    private final ContentScraperService scraperService;

    public ProcessorService(
            RedisStoreService store,
            AnalyticsService analytics,
            GeminiAiService aiService,
            ContentScraperService scraperService
    ) {
        this.store = store;
        this.analytics = analytics;
        this.aiService = aiService;
        this.scraperService = scraperService;
    }

    public static String hashInput(String input) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.substring(0, Math.min(16, hexString.length()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public String getInputHash(String input) {
        return hashInput(input);
    }

    public JobRecord createJob(String jobId, String input) {
        String normalizedInput = input != null ? input.trim() : "";
        JobRecord job = new JobRecord();
        job.setJobId(jobId);
        job.setInput(normalizedInput);
        job.setInputHash(hashInput(normalizedInput));
        job.setStatus(JobStatus.PENDING);
        job.setCreatedAt(Instant.now().toString());
        return job;
    }

    public ProcessResult process(JobRecord job) {
        long startedAt = System.currentTimeMillis();
        String resolvedInput = scraperService.resolveInput(job.getInput());

        try {
            SummaryPayload payload = aiService.summarizeText(resolvedInput);
            long durationMs = System.currentTimeMillis() - startedAt;

            store.setSummary(job.getInputHash(), payload);
            analytics.trackProcessingTime(durationMs);

            return new ProcessResult(payload.getSummary(), payload.getTags(), durationMs);
        } catch (Exception error) {
            analytics.trackFailure();
            log.error("Failed to process job {}: {}", job.getJobId(), error.getMessage());
            if (error instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Job processing failed: " + error.getMessage(), error);
        }
    }

    public record ProcessResult(String summary, List<String> tags, long durationMs) {}
}

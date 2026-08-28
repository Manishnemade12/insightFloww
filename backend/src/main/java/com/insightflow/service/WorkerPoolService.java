package com.insightflow.service;

import com.insightflow.config.AppProperties;
import com.insightflow.model.JobRecord;
import com.insightflow.model.JobStatus;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WorkerPoolService {

    private static final Logger log = LoggerFactory.getLogger(WorkerPoolService.class);

    private final RedisStoreService store;
    private final ProcessorService processorService;
    private final AppProperties properties;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService;

    public WorkerPoolService(RedisStoreService store, ProcessorService processorService, AppProperties properties) {
        this.store = store;
        this.processorService = processorService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (running.get()) {
            return;
        }

        int workerCount = properties.getWorkerCount();
        running.set(true);
        executorService = Executors.newFixedThreadPool(workerCount, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("insightflow-worker-" + thread.getId());
            return thread;
        });

        log.info("Starting {} background workers...", workerCount);

        for (int i = 1; i <= workerCount; i++) {
            final int workerId = i;
            executorService.submit(() -> runWorker(workerId));
        }
    }

    @PreDestroy
    public synchronized void stop() {
        if (!running.get()) {
            return;
        }

        log.info("Shutting down background workers...");
        running.set(false);

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Workers shut down successfully.");
    }

    private void runWorker(int workerId) {
        log.info("Worker {} started listening on queue...", workerId);

        while (running.get()) {
            try {
                String jobId = store.popQueue(Duration.ofSeconds(5));
                if (jobId == null || jobId.isBlank()) {
                    continue;
                }

                processJob(workerId, jobId);
            } catch (Exception e) {
                if (!running.get()) {
                    return;
                }
                log.error("Worker {} queue error: {}", workerId, e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void processJob(int workerId, String jobId) {
        JobRecord job = store.getJob(jobId);
        if (job == null) {
            log.error("Worker {} could not load job {}", workerId, jobId);
            return;
        }

        job.setStatus(JobStatus.PROCESSING);
        store.setJob(job);

        try {
            ProcessorService.ProcessResult result = processorService.process(job);

            job.setSummary(result.summary());
            job.setTags(result.tags());
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now().toString());
            job.setDurationMs(result.durationMs());

            store.setJob(job);
            log.info("Worker {} completed job {} in {}ms", workerId, jobId, result.durationMs());
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setError("Processing error: " + (e.getMessage() != null ? e.getMessage() : "unknown error"));
            job.setCompletedAt(Instant.now().toString());

            store.setJob(job);
            log.error("Worker {} failed job {}: {}", workerId, jobId, job.getError());
        }
    }
}

package com.insightflow.service;

import com.insightflow.model.AnalyticsSnapshot;
import org.springframework.stereotype.Service;

import static com.insightflow.service.RedisStoreService.*;

@Service
public class AnalyticsService {

    private final RedisStoreService store;

    public AnalyticsService(RedisStoreService store) {
        this.store = store;
    }

    public void trackRequest() {
        store.incrMetric(METRIC_REQUESTS);
    }

    public void trackCacheHit() {
        store.incrMetric(METRIC_CACHE_HITS);
    }

    public void trackCacheMiss() {
        store.incrMetric(METRIC_CACHE_MISSES);
    }

    public void trackFailure() {
        store.incrMetric(METRIC_FAILED);
    }

    public void trackProcessingTime(long ms) {
        store.addProcessingTime(ms);
    }

    public AnalyticsSnapshot getMetrics() {
        long totalRequests = store.getMetricLong(METRIC_REQUESTS);
        long cacheHits = store.getMetricLong(METRIC_CACHE_HITS);
        long cacheMisses = store.getMetricLong(METRIC_CACHE_MISSES);
        long failedJobs = store.getMetricLong(METRIC_FAILED);
        long totalTime = store.getMetricLong(METRIC_TOTAL_TIME);
        long completedJobs = store.getMetricLong(METRIC_COMPLETED);
        long queueSize = store.queueSize();

        double avgProcessingTime = completedJobs > 0 ? (double) totalTime / completedJobs : 0.0;

        return new AnalyticsSnapshot(
                totalRequests,
                cacheHits,
                cacheMisses,
                failedJobs,
                queueSize,
                avgProcessingTime
        );
    }
}

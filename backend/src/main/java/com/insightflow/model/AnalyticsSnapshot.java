package com.insightflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class AnalyticsSnapshot implements Serializable {

    @JsonProperty("total_requests")
    private long totalRequests;

    @JsonProperty("cache_hits")
    private long cacheHits;

    @JsonProperty("cache_misses")
    private long cacheMisses;

    @JsonProperty("failed_jobs")
    private long failedJobs;

    @JsonProperty("queue_size")
    private long queueSize;

    @JsonProperty("avg_processing_time_ms")
    private double avgProcessingTimeMs;

    public AnalyticsSnapshot() {}

    public AnalyticsSnapshot(long totalRequests, long cacheHits, long cacheMisses, long failedJobs, long queueSize, double avgProcessingTimeMs) {
        this.totalRequests = totalRequests;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
        this.failedJobs = failedJobs;
        this.queueSize = queueSize;
        this.avgProcessingTimeMs = avgProcessingTimeMs;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getCacheHits() {
        return cacheHits;
    }

    public void setCacheHits(long cacheHits) {
        this.cacheHits = cacheHits;
    }

    public long getCacheMisses() {
        return cacheMisses;
    }

    public void setCacheMisses(long cacheMisses) {
        this.cacheMisses = cacheMisses;
    }

    public long getFailedJobs() {
        return failedJobs;
    }

    public void setFailedJobs(long failedJobs) {
        this.failedJobs = failedJobs;
    }

    public long getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(long queueSize) {
        this.queueSize = queueSize;
    }

    public double getAvgProcessingTimeMs() {
        return avgProcessingTimeMs;
    }

    public void setAvgProcessingTimeMs(double avgProcessingTimeMs) {
        this.avgProcessingTimeMs = avgProcessingTimeMs;
    }
}

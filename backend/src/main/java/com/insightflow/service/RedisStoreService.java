package com.insightflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.config.AppProperties;
import com.insightflow.model.JobRecord;
import com.insightflow.model.SummaryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisStoreService {

    private static final Logger log = LoggerFactory.getLogger(RedisStoreService.class);

    public static final String QUEUE_KEY = "job_queue";
    public static final String METRIC_REQUESTS = "metrics:total_requests";
    public static final String METRIC_CACHE_HITS = "metrics:cache_hits";
    public static final String METRIC_CACHE_MISSES = "metrics:cache_misses";
    public static final String METRIC_FAILED = "metrics:failed_jobs";
    public static final String METRIC_TOTAL_TIME = "metrics:processing_time_ms";
    public static final String METRIC_COMPLETED = "metrics:completed_jobs";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public RedisStoreService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, AppProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public static String summaryKey(String hash) {
        return "summary:" + hash;
    }

    public static String jobKey(String jobId) {
        return "job:" + jobId;
    }

    public SummaryPayload getSummary(String hash) {
        try {
            String raw = redisTemplate.opsForValue().get(summaryKey(hash));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, SummaryPayload.class);
        } catch (Exception e) {
            log.error("Failed to deserialize summary for hash: {}", hash, e);
            return null;
        }
    }

    public void setSummary(String hash, SummaryPayload payload) {
        try {
            String raw = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(
                    summaryKey(hash),
                    raw,
                    Duration.ofSeconds(properties.getCacheTtlSeconds())
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize summary for hash: {}", hash, e);
        }
    }

    public JobRecord getJob(String jobId) {
        try {
            String raw = redisTemplate.opsForValue().get(jobKey(jobId));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, JobRecord.class);
        } catch (Exception e) {
            log.error("Failed to deserialize job for jobId: {}", jobId, e);
            return null;
        }
    }

    public void setJob(JobRecord job) {
        try {
            String raw = objectMapper.writeValueAsString(job);
            redisTemplate.opsForValue().set(
                    jobKey(job.getJobId()),
                    raw,
                    Duration.ofSeconds(properties.getJobTtlSeconds())
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize job for jobId: {}", job.getJobId(), e);
        }
    }

    public void pushQueue(String jobId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, jobId);
    }

    public String popQueue(Duration timeout) {
        return redisTemplate.opsForList().leftPop(QUEUE_KEY, timeout);
    }

    public long queueSize() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return size != null ? size : 0L;
    }

    public void incrMetric(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    public void addProcessingTime(long ms) {
        redisTemplate.opsForValue().increment(METRIC_TOTAL_TIME, ms);
        redisTemplate.opsForValue().increment(METRIC_COMPLETED);
    }

    public long getMetricLong(String key) {
        String val = redisTemplate.opsForValue().get(key);
        if (val == null || val.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}

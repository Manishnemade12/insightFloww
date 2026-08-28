import { REDIS_KEYS, RedisStore } from './redis.js';
import type { AnalyticsSnapshot } from './types.js';

export class AnalyticsTracker {
  constructor(private readonly store: RedisStore) {}

  trackRequest(): Promise<void> {
    return this.store.incrMetric(REDIS_KEYS.requests);
  }

  trackCacheHit(): Promise<void> {
    return this.store.incrMetric(REDIS_KEYS.cacheHits);
  }

  trackCacheMiss(): Promise<void> {
    return this.store.incrMetric(REDIS_KEYS.cacheMisses);
  }

  trackFailure(): Promise<void> {
    return this.store.incrMetric(REDIS_KEYS.failed);
  }

  trackProcessingTime(ms: number): Promise<void> {
    return this.store.addProcessingTime(ms);
  }

  async getMetrics(): Promise<AnalyticsSnapshot> {
    const [totalRequests, cacheHits, cacheMisses, failedJobs, totalTime, completedJobs, queueSize] = await Promise.all([
      this.store.getMetricLong(REDIS_KEYS.requests),
      this.store.getMetricLong(REDIS_KEYS.cacheHits),
      this.store.getMetricLong(REDIS_KEYS.cacheMisses),
      this.store.getMetricLong(REDIS_KEYS.failed),
      this.store.getMetricLong(REDIS_KEYS.totalTime),
      this.store.getMetricLong(REDIS_KEYS.completed),
      this.store.queueSize(),
    ]);

    return {
      total_requests: totalRequests,
      cache_hits: cacheHits,
      cache_misses: cacheMisses,
      failed_jobs: failedJobs,
      queue_size: queueSize,
      avg_processing_time_ms: completedJobs > 0 ? totalTime / completedJobs : 0,
    };
  }
}
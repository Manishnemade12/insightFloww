import Redis from 'ioredis';

import { config } from './config.js';
import type { JobRecord, SummaryPayload } from './types.js';

export const REDIS_KEYS = {
  summary: (hash: string) => `summary:${hash}`,
  job: (jobId: string) => `job:${jobId}`,
  queue: 'job_queue',
  requests: 'metrics:total_requests',
  cacheHits: 'metrics:cache_hits',
  cacheMisses: 'metrics:cache_misses',
  failed: 'metrics:failed_jobs',
  totalTime: 'metrics:processing_time_ms',
  completed: 'metrics:completed_jobs',
} as const;

export class RedisStore {
  private readonly client: any;

  constructor() {
    const RedisClient = Redis as unknown as new (url: string, options: Record<string, unknown>) => any;
    this.client = new RedisClient(config.redisUrl, {
      maxRetriesPerRequest: null,
      enableReadyCheck: true,
      lazyConnect: true,
    });
  }

  async connect(): Promise<void> {
    await this.client.connect();
    await this.client.ping();
  }

  async disconnect(): Promise<void> {
    await this.client.quit();
  }

  createBlockingClient(): any {
    const blockingClient = this.client.duplicate({
      maxRetriesPerRequest: null,
      lazyConnect: true,
    });

    return blockingClient;
  }

  async getSummary(hash: string): Promise<SummaryPayload | null> {
    const raw = await this.client.get(REDIS_KEYS.summary(hash));
    return raw ? (JSON.parse(raw) as SummaryPayload) : null;
  }

  async setSummary(hash: string, payload: SummaryPayload): Promise<void> {
    await this.client.set(REDIS_KEYS.summary(hash), JSON.stringify(payload), 'EX', config.cacheTtlSeconds);
  }

  async getJob(jobId: string): Promise<JobRecord | null> {
    const raw = await this.client.get(REDIS_KEYS.job(jobId));
    return raw ? (JSON.parse(raw) as JobRecord) : null;
  }

  async setJob(job: JobRecord): Promise<void> {
    await this.client.set(REDIS_KEYS.job(job.job_id), JSON.stringify(job), 'EX', config.jobTtlSeconds);
  }

  async pushQueue(jobId: string): Promise<void> {
    await this.client.rpush(REDIS_KEYS.queue, jobId);
  }

  async popQueue(blockingClient: any, timeoutSeconds = 5): Promise<string | null> {
    const result = await blockingClient.blpop(REDIS_KEYS.queue, timeoutSeconds);
    return result ? result[1] : null;
  }

  async queueSize(): Promise<number> {
    return this.client.llen(REDIS_KEYS.queue);
  }

  async incrMetric(key: string): Promise<void> {
    await this.client.incr(key);
  }

  async addProcessingTime(ms: number): Promise<void> {
    await this.client.incrby(REDIS_KEYS.totalTime, ms);
    await this.client.incr(REDIS_KEYS.completed);
  }

  async getMetricLong(key: string): Promise<number> {
    const value = await this.client.get(key);
    if (!value) {
      return 0;
    }

    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) ? parsed : 0;
  }
}
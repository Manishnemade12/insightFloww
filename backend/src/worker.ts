import { setTimeout as delay } from 'node:timers/promises';
import type { Redis as IORedis } from 'ioredis';

import { RedisStore } from './redis.js';
import { ProcessorService } from './processor.js';

export class WorkerPool {
  private running = false;
  private workers: Promise<void>[] = [];
  private blockingClients: any[] = [];

  constructor(
    private readonly store: RedisStore,
    private readonly processorService: ProcessorService,
    private readonly workerCount: number,
  ) {}

  async start(): Promise<void> {
    if (this.running) {
      return;
    }

    this.running = true;

    for (let index = 0; index < this.workerCount; index += 1) {
      const blockingClient = this.store.createBlockingClient();
      await blockingClient.connect();
      this.blockingClients.push(blockingClient);
      this.workers.push(this.runWorker(index + 1, blockingClient));
    }
  }

  async stop(): Promise<void> {
    this.running = false;
    await Promise.allSettled(this.blockingClients.map(client => client.quit()));
    await Promise.allSettled(this.workers);
  }

  private async runWorker(workerId: number, blockingClient: any): Promise<void> {
    while (this.running) {
      try {
        const jobId = await this.store.popQueue(blockingClient, 5);
        if (!jobId) {
          continue;
        }

        await this.processJob(workerId, jobId);
      } catch (error) {
        if (!this.running) {
          return;
        }

        console.error(`Worker ${workerId} queue error: ${error instanceof Error ? error.message : 'unknown error'}`);
        await delay(1000);
      }
    }
  }

  private async processJob(workerId: number, jobId: string): Promise<void> {
    const job = await this.store.getJob(jobId);
    if (!job) {
      console.error(`Worker ${workerId} could not load job ${jobId}`);
      return;
    }

    job.status = 'processing';
    await this.store.setJob(job);

    try {
      const result = await this.processorService.process(job);

      job.summary = result.summary;
      job.tags = result.tags;
      job.status = 'completed';
      job.completed_at = new Date().toISOString();
      job.duration_ms = result.durationMs;

      await this.store.setJob(job);
      console.log(`Worker ${workerId} completed job ${jobId} in ${result.durationMs}ms`);
    } catch (error) {
      job.status = 'failed';
      job.error = `Processing error: ${error instanceof Error ? error.message : 'unknown error'}`;
      job.completed_at = new Date().toISOString();

      await this.store.setJob(job);
      console.error(`Worker ${workerId} failed job ${jobId}: ${job.error}`);
    }
  }
}
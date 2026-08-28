import cors from 'cors';
import express from 'express';
import type { NextFunction, Request, Response } from 'express';
import { randomUUID } from 'node:crypto';

import { AnalyticsTracker } from './analytics.js';
import { config } from './config.js';
import { ProcessorService } from './processor.js';
import { RedisStore } from './redis.js';
import { WorkerPool } from './worker.js';

const app = express();
const store = new RedisStore();
const analytics = new AnalyticsTracker(store);
const processorService = new ProcessorService(store, analytics);
const workerPool = new WorkerPool(store, processorService, config.workerCount);

app.use(cors({ origin: config.corsOrigin === '*' ? true : config.corsOrigin.split(',').map(origin => origin.trim()) }));
app.use(express.json({ limit: '1mb' }));

app.get('/api/health', (_request, response) => {
  response.json({ status: 'ok', time: new Date().toISOString(), service: config.appName });
});

app.post('/api/submit', async (request, response) => {
  try {
    await analytics.trackRequest();

    const input = typeof request.body?.input === 'string' ? request.body.input.trim() : '';
    if (!input) {
      response.status(400).json({ error: 'input field is required' });
      return;
    }

    const inputHash = processorService.getInputHash(input);
    const cachedSummary = await store.getSummary(inputHash);

    if (cachedSummary) {
      await analytics.trackCacheHit();
      const parsed = cachedSummary;

      response.json({
        job_id: inputHash,
        status: 'completed',
        cached: true,
        summary: parsed.summary,
        tags: parsed.tags ?? [],
      });
      return;
    }

    await analytics.trackCacheMiss();

    const jobId = randomUUID();
    const job = processorService.createJob(jobId, input);

    await store.setJob(job);
    await store.pushQueue(jobId);

    response.status(202).json({
      job_id: jobId,
      status: 'pending',
      cached: false,
    });
  } catch (error) {
    response.status(500).json({ error: error instanceof Error ? error.message : 'failed to submit job' });
  }
});

app.get('/api/status/:jobId', async (request, response) => {
  try {
    const jobId = request.params.jobId.trim();
    if (!jobId) {
      response.status(400).json({ error: 'job_id is required' });
      return;
    }

    const job = await store.getJob(jobId);
    if (!job) {
      response.status(404).json({ error: 'job not found' });
      return;
    }

    response.json(job);
  } catch (error) {
    response.status(500).json({ error: error instanceof Error ? error.message : 'failed to fetch job' });
  }
});

app.get('/api/analytics', async (_request, response) => {
  try {
    response.json(await analytics.getMetrics());
  } catch (error) {
    response.status(500).json({ error: error instanceof Error ? error.message : 'failed to fetch analytics' });
  }
});

app.use((error: unknown, _request: Request, response: Response, _next: NextFunction) => {
  console.error(error);
  response.status(500).json({ error: 'internal server error' });
});

async function bootstrap(): Promise<void> {
  await store.connect();
  await workerPool.start();

  const server = app.listen(config.port, () => {
    console.log(`${config.appName} listening on http://localhost:${config.port}`);
  });

  const shutdown = async () => {
    await workerPool.stop();
    await store.disconnect();
    server.close(() => process.exit(0));
  };

  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);
}

bootstrap().catch(error => {
  console.error('Failed to start backend:', error);
  process.exit(1);
});
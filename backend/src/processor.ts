import { createHash } from 'node:crypto';

import { summarizeText } from './ai.js';
import { AnalyticsTracker } from './analytics.js';
import { RedisStore } from './redis.js';
import type { JobRecord } from './types.js';

function hashInput(input: string): string {
  return createHash('sha256').update(input.toLowerCase().trim()).digest('hex').slice(0, 16);
}

function stripHtml(html: string): string {
  let result = '';
  let inTag = false;

  for (const char of html) {
    if (char === '<') {
      inTag = true;
      continue;
    }

    if (char === '>') {
      inTag = false;
      result += ' ';
      continue;
    }

    if (!inTag) {
      result += char;
    }
  }

  return result.trim().replace(/\s+/g, ' ');
}

async function fetchUrl(url: string): Promise<string> {
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'user-agent': 'InsightFlowAI/1.0',
      accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch URL: HTTP ${response.status}`);
  }

  const body = await response.text();
  const limitedBody = body.length > 50_000 ? body.slice(0, 50_000) : body;
  const text = stripHtml(limitedBody);
  return text.length > 8_000 ? text.slice(0, 8_000) : text;
}

async function resolveInput(input: string): Promise<string> {
  const trimmed = input.trim();
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return fetchUrl(trimmed);
  }

  return trimmed;
}

export class ProcessorService {
  constructor(
    private readonly store: RedisStore,
    private readonly analytics: AnalyticsTracker,
  ) {}

  getInputHash(input: string): string {
    return hashInput(input);
  }

  async process(job: JobRecord): Promise<{ summary: string; tags: string[]; durationMs: number }> {
    const startedAt = Date.now();
    const resolvedInput = await resolveInput(job.input);

    try {
      const payload = await summarizeText(resolvedInput);
      const durationMs = Date.now() - startedAt;

      await this.store.setSummary(job.input_hash, payload);
      await this.analytics.trackProcessingTime(durationMs);

      return {
        summary: payload.summary,
        tags: payload.tags,
        durationMs,
      };
    } catch (error) {
      await this.analytics.trackFailure();
      throw error instanceof Error ? error : new Error('Unknown processing error');
    }
  }

  createJob(jobId: string, input: string): JobRecord {
    const normalizedInput = input.trim();

    return {
      job_id: jobId,
      input: normalizedInput,
      input_hash: hashInput(normalizedInput),
      status: 'pending',
      created_at: new Date().toISOString(),
    };
  }
}
import dotenv from 'dotenv';

dotenv.config();

function toInt(value: string | undefined, fallback: number): number {
  const parsed = Number.parseInt(value ?? '', 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export const config = {
  appName: 'InsightFlow AI',
  port: toInt(process.env.PORT, 8080),
  redisUrl: process.env.REDIS_URL ?? 'redis://localhost:6379',
  geminiApiKey: process.env.GEMINI_API_KEY ?? '',
  geminiModel: process.env.GEMINI_MODEL ?? 'gemini-1.5-flash',
  workerCount: Math.max(1, toInt(process.env.WORKER_COUNT, 3)),
  cacheTtlSeconds: Math.max(60, toInt(process.env.CACHE_TTL, 300)),
  jobTtlSeconds: Math.max(3600, toInt(process.env.JOB_TTL_SECONDS, 24 * 60 * 60)),
  corsOrigin: process.env.CORS_ORIGIN ?? '*',
};
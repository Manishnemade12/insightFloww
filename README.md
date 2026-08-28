# ⚡ InsightFlow AI

> **Async AI summarization and caching engine** — Java 21 · Spring Boot 3.4 · Redis / Valkey · Gemini · React

InsightFlow AI is a full-stack app that accepts text or URL input, queues work in Redis, processes summaries in a multi-threaded worker pool, and returns AI-generated summaries with tags.

## Overview

The backend exposes a Spring Boot REST API and a multi-threaded queue worker pool. The frontend submits an input, polls for status, and shows summary results or cache hits. Redis / Valkey stores job state, queue entries, metrics, and cached summaries.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21 · Spring Boot 3.4 (REST API, Redis Lettuce client, multi-worker pool) |
| Queue + Cache | Redis 7+ / Valkey 8 · Spring Data Redis |
| AI | Google Gemini 1.5 Flash via REST |
| Frontend | React 19 · TypeScript · Vite 5 |

## How It Works

1. `POST /api/submit` receives text or a URL.
2. The backend hashes the normalized input and checks `summary:{hash}` in Redis.
3. Cache hit returns the saved summary immediately.
4. Cache miss creates a job record, pushes it onto `job_queue`, and returns `pending`.
5. A background worker pops the job, resolves URL content if needed, calls Gemini, stores the result, and marks the job completed.
6. The frontend polls `GET /api/status/:jobId` until the job completes.

## Local Setup

```bash
# 1. Start Valkey / Redis
docker compose up -d

# 2. Configure Backend environment
cp backend/.env.example backend/.env
# Update GEMINI_API_KEY in backend/.env

# 3. Start Spring Boot Backend
cd backend
./mvnw spring-boot:run
# On Windows PowerShell:
# .\mvnw.cmd spring-boot:run
```

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Backend defaults to `http://localhost:8080`. Frontend defaults to `http://localhost:5173`.

## API

### `POST /api/submit`

Request:

```json
{ "input": "Paste your text or https://example.com/article" }
```

Cache miss response:

```json
{ "job_id": "uuid", "status": "pending", "cached": false }
```

Cache hit response:

```json
{ "job_id": "hash", "status": "completed", "cached": true, "summary": "...", "tags": [] }
```

### `GET /api/status/:jobId`

```json
{
  "job_id": "...",
  "status": "completed",
  "summary": "...",
  "tags": ["AI"],
  "duration_ms": 1230
}
```

### `GET /api/analytics`

```json
{
  "total_requests": 42,
  "cache_hits": 30,
  "cache_misses": 12,
  "failed_jobs": 1,
  "queue_size": 0,
  "avg_processing_time_ms": 1240
}
```

### `GET /api/health`

Returns a simple `ok` response with a timestamp.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | Backend port |
| `REDIS_URL` | `redis://localhost:6379` | Redis / Valkey connection URL |
| `GEMINI_API_KEY` | required | Google AI Studio API key |
| `GEMINI_MODEL` | `gemini-1.5-flash` | Gemini model to call |
| `WORKER_COUNT` | `3` | Number of worker loops |
| `CACHE_TTL` | `300` | Summary cache TTL in seconds |
| `JOB_TTL_SECONDS` | `86400` | Job record TTL in seconds |
| `CORS_ORIGIN` | `http://localhost:5173` | Allowed frontend origin |

## Redis Key Design

| Key Pattern | Purpose |
|------------|---------|
| `summary:{hash}` | Cached AI result |
| `job:{id}` | Job state |
| `job_queue` | FIFO queue of pending jobs |
| `metrics:*` | Analytics counters |

## Project Structure

```text
insightflow-ai/
├── backend/
│   ├── src/
│   │   ├── server.ts
│   │   ├── worker.ts
│   │   ├── processor.ts
│   │   ├── ai.ts
│   │   ├── redis.ts
│   │   ├── analytics.ts
│   │   └── config.ts
│   ├── .env.example
│   ├── package.json
│   └── tsconfig.json
├── frontend/
│   └── src/
└── docker-compose.yml
```
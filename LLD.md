# InsightFlow AI LLD

## Purpose

InsightFlow AI is a single-service Node.js backend plus React frontend for async AI summarization. The design emphasizes queue-backed processing, Redis caching, and a simple API contract that can be polled from the UI.

## Architecture

Client -> Express API -> Redis / Valkey -> Worker pool -> Gemini -> Redis / Valkey -> Client

## Core Flow

1. User submits text or a URL.
2. The backend normalizes the input and hashes it.
3. Redis is checked for `summary:{hash}`.
4. On a cache hit, the API returns the cached summary immediately.
5. On a cache miss, a job record is stored and the job ID is pushed onto `job_queue`.
6. Background workers read from the queue, fetch URL content when needed, call Gemini, persist the summary, and update job status.

## Major Components

### API Layer

Express routes:

- `POST /api/submit`
- `GET /api/status/:jobId`
- `GET /api/analytics`
- `GET /api/health`

### Redis Layer

Redis / Valkey stores:

- `summary:{hash}` for cached AI results
- `job:{id}` for job records
- `job_queue` for pending work
- `metrics:*` for request and processing counters

### Worker Layer

The worker pool uses a fixed number of loops. Each loop blocks on Redis with `BLPOP`, updates the job to `processing`, calls the processor, and writes the completed or failed state back to Redis.

### AI Layer

Gemini receives a prompt that requires JSON output with:

- `summary`
- `tags`

### Analytics Layer

The metrics response includes:

- total requests
- cache hits
- cache misses
- failed jobs
- queue size
- average processing time

## Environment

```bash
PORT=8080
REDIS_URL=redis://localhost:6379
GEMINI_API_KEY=your_key
GEMINI_MODEL=gemini-1.5-flash
WORKER_COUNT=3
CACHE_TTL=300
JOB_TTL_SECONDS=86400
CORS_ORIGIN=http://localhost:5173
```

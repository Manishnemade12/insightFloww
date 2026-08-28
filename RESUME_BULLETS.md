• Built an async job pipeline with Node.js worker pool and Redis/Valkey queue (BLPOP) for non-blocking AI summarization.
• Applied SHA-256 deduplication to eliminate redundant Gemini API calls on repeated text/URL inputs.
• Designed Express REST APIs with cache-aside pattern, instant cache hits, async polling via job id on misses.
• Built React 19 + TypeScript frontend with real-time job status polling and a live analytics dashboard for cache metrics.
export type JobStatus = 'pending' | 'processing' | 'completed' | 'failed';

export interface SummaryPayload {
  summary: string;
  tags: string[];
}

export interface JobRecord {
  job_id: string;
  input: string;
  input_hash: string;
  status: JobStatus;
  summary?: string;
  tags?: string[];
  error?: string;
  created_at: string;
  completed_at?: string;
  duration_ms?: number;
}

export interface AnalyticsSnapshot {
  total_requests: number;
  cache_hits: number;
  cache_misses: number;
  failed_jobs: number;
  queue_size: number;
  avg_processing_time_ms: number;
}
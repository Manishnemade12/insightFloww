package com.insightflow.service;

import com.insightflow.model.JobRecord;
import com.insightflow.model.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorServiceTest {

    private ProcessorService processorService;
    private RedisStoreService store;
    private AnalyticsService analytics;
    private GeminiAiService aiService;
    private ContentScraperService scraperService;

    @BeforeEach
    void setUp() {
        store = Mockito.mock(RedisStoreService.class);
        analytics = Mockito.mock(AnalyticsService.class);
        aiService = Mockito.mock(GeminiAiService.class);
        scraperService = new ContentScraperService();
        processorService = new ProcessorService(store, analytics, aiService, scraperService);
    }

    @Test
    void testHashInputGenerates16CharHex() {
        String hash = ProcessorService.hashInput("Sample Text for Summarization");
        assertNotNull(hash);
        assertEquals(16, hash.length());
    }

    @Test
    void testHashInputIsCaseInsensitiveAndTrimmed() {
        String hash1 = ProcessorService.hashInput("   AI Machine Learning   ");
        String hash2 = ProcessorService.hashInput("ai machine learning");
        assertEquals(hash1, hash2);
    }

    @Test
    void testCreateJobInitializesPendingJob() {
        JobRecord job = processorService.createJob("test-job-123", "   Test input text   ");

        assertEquals("test-job-123", job.getJobId());
        assertEquals("Test input text", job.getInput());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertNotNull(job.getInputHash());
        assertEquals(16, job.getInputHash().length());
        assertNotNull(job.getCreatedAt());
    }
}

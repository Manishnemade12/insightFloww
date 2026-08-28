package com.insightflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.config.AppProperties;
import com.insightflow.model.JobRecord;
import com.insightflow.model.JobStatus;
import com.insightflow.model.SubmitRequest;
import com.insightflow.model.SummaryPayload;
import com.insightflow.service.AnalyticsService;
import com.insightflow.service.ProcessorService;
import com.insightflow.service.RedisStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@Import(AppProperties.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisStoreService store;

    @MockBean
    private ProcessorService processorService;

    @MockBean
    private AnalyticsService analytics;

    @Test
    void testSubmitCacheMissReturns202Accepted() throws Exception {
        SubmitRequest request = new SubmitRequest("New text to summarize");
        when(processorService.getInputHash("New text to summarize")).thenReturn("abc123hash");
        when(store.getSummary("abc123hash")).thenReturn(null);

        JobRecord mockJob = new JobRecord();
        mockJob.setJobId("job-uuid-1");
        mockJob.setStatus(JobStatus.PENDING);
        when(processorService.createJob(any(), eq("New text to summarize"))).thenReturn(mockJob);

        mockMvc.perform(post("/api/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.cached").value(false))
                .andExpect(jsonPath("$.job_id").exists());

        verify(analytics).trackRequest();
        verify(analytics).trackCacheMiss();
        verify(store).pushQueue(any());
    }

    @Test
    void testSubmitCacheHitReturns200OkWithSummary() throws Exception {
        SubmitRequest request = new SubmitRequest("Existing text");
        when(processorService.getInputHash("Existing text")).thenReturn("cachedhash123");
        when(store.getSummary("cachedhash123")).thenReturn(
                new SummaryPayload("This is the cached summary.", List.of("AI", "Testing"))
        );

        mockMvc.perform(post("/api/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_id").value("cachedhash123"))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.cached").value(true))
                .andExpect(jsonPath("$.summary").value("This is the cached summary."))
                .andExpect(jsonPath("$.tags[0]").value("AI"));

        verify(analytics).trackRequest();
        verify(analytics).trackCacheHit();
    }

    @Test
    void testSubmitEmptyInputReturns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("input field is required"));
    }

    @Test
    void testGetJobStatusFoundReturns200() throws Exception {
        JobRecord job = new JobRecord();
        job.setJobId("job-123");
        job.setStatus(JobStatus.COMPLETED);
        job.setSummary("Summary done");
        when(store.getJob("job-123")).thenReturn(job);

        mockMvc.perform(get("/api/status/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_id").value("job-123"))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.summary").value("Summary done"));
    }

    @Test
    void testGetJobStatusNotFoundReturns404() throws Exception {
        when(store.getJob("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/api/status/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("job not found"));
    }
}

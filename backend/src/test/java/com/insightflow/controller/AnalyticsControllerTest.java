package com.insightflow.controller;

import com.insightflow.config.AppProperties;
import com.insightflow.model.AnalyticsSnapshot;
import com.insightflow.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@Import(AppProperties.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void testGetAnalyticsReturnsMetrics() throws Exception {
        AnalyticsSnapshot snapshot = new AnalyticsSnapshot(42, 30, 12, 1, 0, 1240.5);
        when(analyticsService.getMetrics()).thenReturn(snapshot);

        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_requests").value(42))
                .andExpect(jsonPath("$.cache_hits").value(30))
                .andExpect(jsonPath("$.cache_misses").value(12))
                .andExpect(jsonPath("$.failed_jobs").value(1))
                .andExpect(jsonPath("$.queue_size").value(0))
                .andExpect(jsonPath("$.avg_processing_time_ms").value(1240.5));
    }
}

package com.insightflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightflow.config.AppProperties;
import com.insightflow.model.GeminiModels;
import com.insightflow.model.SummaryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GeminiAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiAiService(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private String buildPrompt(String text) {
        return "You are a concise summarization assistant.\n\n" +
                "Summarize the following text in 2-3 sentences and extract 2-4 relevant tags.\n\n" +
                "Return ONLY valid JSON in this exact format (no markdown, no code blocks):\n" +
                "{\n" +
                "  \"summary\": \"Your 2-3 sentence summary here.\",\n" +
                "  \"tags\": [\"tag1\", \"tag2\", \"tag3\"]\n" +
                "}\n\n" +
                "TEXT TO SUMMARIZE:\n" +
                text;
    }

    private String cleanResponseText(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    public SummaryPayload summarizeText(String text) {
        String apiKey = properties.getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        try {
            String prompt = buildPrompt(text);
            GeminiModels.Request requestBody = new GeminiModels.Request(prompt, 0.3, "application/json");
            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    properties.getGeminiModel(),
                    apiKey
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API error: HTTP " + response.statusCode() + " " + response.body().trim());
            }

            GeminiModels.Response geminiResponse = objectMapper.readValue(response.body(), GeminiModels.Response.class);
            String rawText = geminiResponse.getFirstText();

            if (rawText == null || rawText.isBlank()) {
                throw new RuntimeException("Empty response from Gemini");
            }

            String cleanedJson = cleanResponseText(rawText);
            return objectMapper.readValue(cleanedJson, SummaryPayload.class);
        } catch (Exception e) {
            log.error("Failed to summarize text with Gemini: {}", e.getMessage());
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("AI summarization failed: " + e.getMessage(), e);
        }
    }
}

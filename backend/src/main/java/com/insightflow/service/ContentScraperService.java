package com.insightflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ContentScraperService {

    private static final Logger log = LoggerFactory.getLogger(ContentScraperService.class);

    private final HttpClient httpClient;

    public ContentScraperService() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String resolveInput(String input) {
        String trimmed = input != null ? input.trim() : "";
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return fetchUrl(trimmed);
        }
        return trimmed;
    }

    public String fetchUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "InsightFlowAI/1.0")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Failed to fetch URL: HTTP " + response.statusCode());
            }

            String body = response.body();
            if (body == null) {
                return "";
            }

            String limitedBody = body.length() > 50_000 ? body.substring(0, 50_000) : body;
            String text = stripHtml(limitedBody);
            return text.length() > 8_000 ? text.substring(0, 8_000) : text;
        } catch (Exception e) {
            log.error("Error fetching URL {}: {}", url, e.getMessage());
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Failed to fetch URL: " + e.getMessage(), e);
        }
    }

    public String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}

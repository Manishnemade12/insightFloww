package com.insightflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "insightflow")
public class AppProperties {

    private String appName = "InsightFlow AI";
    private String geminiApiKey = "";
    private String geminiModel = "gemini-1.5-flash";
    private int workerCount = 3;
    private long cacheTtlSeconds = 300;
    private long jobTtlSeconds = 86400;
    private String corsOrigin = "*";

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel;
    }

    public int getWorkerCount() {
        return Math.max(1, workerCount);
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public long getCacheTtlSeconds() {
        return Math.max(60, cacheTtlSeconds);
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public long getJobTtlSeconds() {
        return Math.max(3600, jobTtlSeconds);
    }

    public void setJobTtlSeconds(long jobTtlSeconds) {
        this.jobTtlSeconds = jobTtlSeconds;
    }

    public String getCorsOrigin() {
        return corsOrigin;
    }

    public void setCorsOrigin(String corsOrigin) {
        this.corsOrigin = corsOrigin;
    }
}

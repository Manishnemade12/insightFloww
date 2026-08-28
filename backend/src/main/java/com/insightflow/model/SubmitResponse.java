package com.insightflow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitResponse {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("cached")
    private boolean cached;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("tags")
    private List<String> tags;

    public SubmitResponse() {}

    public SubmitResponse(String jobId, String status, boolean cached) {
        this.jobId = jobId;
        this.status = status;
        this.cached = cached;
    }

    public SubmitResponse(String jobId, String status, boolean cached, String summary, List<String> tags) {
        this.jobId = jobId;
        this.status = status;
        this.cached = cached;
        this.summary = summary;
        this.tags = tags;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

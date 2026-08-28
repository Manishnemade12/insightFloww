package com.insightflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SummaryPayload implements Serializable {

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    public SummaryPayload() {}

    public SummaryPayload(String summary, List<String> tags) {
        this.summary = summary;
        this.tags = tags != null ? tags : new ArrayList<>();
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
        this.tags = tags != null ? tags : new ArrayList<>();
    }
}

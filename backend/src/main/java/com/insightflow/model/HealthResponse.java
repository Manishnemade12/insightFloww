package com.insightflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("time")
    private String time;

    @JsonProperty("service")
    private String service;

    public HealthResponse() {}

    public HealthResponse(String status, String time, String service) {
        this.status = status;
        this.time = time;
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }
}

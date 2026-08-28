package com.insightflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class SubmitRequest {

    @NotBlank(message = "input field is required")
    @JsonProperty("input")
    private String input;

    public SubmitRequest() {}

    public SubmitRequest(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}

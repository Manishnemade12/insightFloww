package com.insightflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

public final class GeminiModels {

    private GeminiModels() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Request {
        @JsonProperty("contents")
        private List<Content> contents;

        @JsonProperty("generationConfig")
        private GenerationConfig generationConfig;

        public Request() {}

        public Request(String prompt, double temperature, String responseMimeType) {
            this.contents = Collections.singletonList(new Content(Collections.singletonList(new Part(prompt))));
            this.generationConfig = new GenerationConfig(temperature, responseMimeType);
        }

        public List<Content> getContents() {
            return contents;
        }

        public void setContents(List<Content> contents) {
            this.contents = contents;
        }

        public GenerationConfig getGenerationConfig() {
            return generationConfig;
        }

        public void setGenerationConfig(GenerationConfig generationConfig) {
            this.generationConfig = generationConfig;
        }
    }

    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;

        public Content() {}

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        @JsonProperty("text")
        private String text;

        public Part() {}

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class GenerationConfig {
        @JsonProperty("temperature")
        private double temperature;

        @JsonProperty("responseMimeType")
        private String responseMimeType;

        public GenerationConfig() {}

        public GenerationConfig(double temperature, String responseMimeType) {
            this.temperature = temperature;
            this.responseMimeType = responseMimeType;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public String getResponseMimeType() {
            return responseMimeType;
        }

        public void setResponseMimeType(String responseMimeType) {
            this.responseMimeType = responseMimeType;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        @JsonProperty("candidates")
        private List<Candidate> candidates;

        public Response() {}

        public List<Candidate> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<Candidate> candidates) {
            this.candidates = candidates;
        }

        public String getFirstText() {
            if (candidates != null && !candidates.isEmpty()) {
                Candidate candidate = candidates.get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null) {
                    for (Part part : candidate.getContent().getParts()) {
                        if (part.getText() != null && !part.getText().isBlank()) {
                            return part.getText();
                        }
                    }
                }
            }
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        @JsonProperty("content")
        private Content content;

        public Candidate() {}

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }
    }
}

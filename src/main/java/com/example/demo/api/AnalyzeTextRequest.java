package com.example.demo.api;

public class AnalyzeTextRequest {

    private String text;

    public AnalyzeTextRequest() {
    }

    public AnalyzeTextRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

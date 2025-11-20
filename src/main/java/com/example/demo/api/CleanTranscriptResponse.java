package com.example.demo.api;

public class CleanTranscriptResponse {

    // 清洗後可直接餵給 AI 的純文字
    private String cleanedText;

    public CleanTranscriptResponse() {
    }

    public CleanTranscriptResponse(String cleanedText) {
        this.cleanedText = cleanedText;
    }

    public String getCleanedText() {
        return cleanedText;
    }

    public void setCleanedText(String cleanedText) {
        this.cleanedText = cleanedText;
    }
}

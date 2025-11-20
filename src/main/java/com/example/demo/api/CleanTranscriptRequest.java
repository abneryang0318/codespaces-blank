package com.example.demo.api;

public class CleanTranscriptRequest {

    // 原始 VTT / SRT / 逐字稿內容
    private String rawText;

    public CleanTranscriptRequest() {
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}

package com.example.demo.model;

public class SectorView {

    // 產業／主題名稱，例如 "AI 半導體", "雲端", "電動車"
    private String name;

    /**
     * 對該產業的整體情緒：
     * - "bullish" / "bearish" / "neutral"
     */
    private String sentiment;

    // 簡短理由
    private String reason;

    public SectorView() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

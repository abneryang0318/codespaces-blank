package com.example.demo.model;

public class TickerView {

    // 股票代碼，例如 TSLA, NVDA
    private String symbol;

    // 股票名稱（可選，例如 Tesla, NVIDIA），如果沒法判斷可以為 null
    private String name;

    /**
     * 講者對該股票的情緒：
     * - "bullish"（偏多）
     * - "bearish"（偏空）
     * - "neutral"（中立或觀望）
     */
    private String sentiment;

    // 為什麼會這樣判斷的簡短理由
    private String reason;

    public TickerView() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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

package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FinMindPriceRecord {

    // 例如 "2024-08-16"
    private String date;

    @JsonProperty("stock_id")
    private String stockId;

    @JsonProperty("Trading_Volume")
    private long tradingVolume;

    private double open;

    // FinMind 用 max / min 代表最高 / 最低
    @JsonProperty("max")
    private double high;

    @JsonProperty("min")
    private double low;

    private double close;

    public FinMindPriceRecord() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStockId() {
        return stockId;
    }

    public void setStockId(String stockId) {
        this.stockId = stockId;
    }

    public long getTradingVolume() {
        return tradingVolume;
    }

    public void setTradingVolume(long tradingVolume) {
        this.tradingVolume = tradingVolume;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }
}

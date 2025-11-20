package com.example.demo.model;

public class StockTaSummary {

    // 股票代號，例如 2330
    private String symbol;

    // 市場類型，這裡先寫死 "TW"，之後要支援 US 再擴充
    private String market;

    // 分析使用的起訖日期（字串，方便直接看）
    private String startDate;
    private String endDate;

    // 最近一根 K 線的收盤價
    private double lastClose;

    // 20 日 SMA
    private double sma20;

    // 14 日 RSI
    private double rsi14;

    // 收盤價是否站上 20 日線
    private boolean aboveSma20;

    // 附帶訊息（成功 / 失敗原因等）
    private String message;

    public StockTaSummary() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public double getLastClose() {
        return lastClose;
    }

    public void setLastClose(double lastClose) {
        this.lastClose = lastClose;
    }

    public double getSma20() {
        return sma20;
    }

    public void setSma20(double sma20) {
        this.sma20 = sma20;
    }

    public double getRsi14() {
        return rsi14;
    }

    public void setRsi14(double rsi14) {
        this.rsi14 = rsi14;
    }

    public boolean isAboveSma20() {
        return aboveSma20;
    }

    public void setAboveSma20(boolean aboveSma20) {
        this.aboveSma20 = aboveSma20;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

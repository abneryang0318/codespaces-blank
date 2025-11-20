package com.example.demo.model;

import java.util.List;

public class PodcastAnalysisResult {

    // 對整段內容的總結（講了什麼、主軸是什麼）
    private String summary;

    // 抓到的股票清單與對應情緒
    private List<TickerView> tickers;

    // 產業／主題的多空看法
    private List<SectorView> sectors;

    // 宏觀觀點的關鍵句，例如 "市場目前關注利率" 等
    private List<String> macroView;

    public PodcastAnalysisResult() {
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<TickerView> getTickers() {
        return tickers;
    }

    public void setTickers(List<TickerView> tickers) {
        this.tickers = tickers;
    }

    public List<SectorView> getSectors() {
        return sectors;
    }

    public void setSectors(List<SectorView> sectors) {
        this.sectors = sectors;
    }

    public List<String> getMacroView() {
        return macroView;
    }

    public void setMacroView(List<String> macroView) {
        this.macroView = macroView;
    }
}

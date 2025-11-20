package com.example.demo.model;

import java.util.Map;

public class PodcastWithTaResponse {

    // 原本的文字分析結果
    private PodcastAnalysisResult analysis;

    // key = 股票代號，例如 "2330"
    private Map<String, StockTaSummary> technicals;

    // 補充說明（例如哪些股票有做 TA、哪些被略過）
    private String message;
        private String finalAnswer;

    public PodcastWithTaResponse() {
    }

    public PodcastAnalysisResult getAnalysis() {
        return analysis;
    }

    public void setAnalysis(PodcastAnalysisResult analysis) {
        this.analysis = analysis;
    }

    public Map<String, StockTaSummary> getTechnicals() {
        return technicals;
    }

    public void setTechnicals(Map<String, StockTaSummary> technicals) {
        this.technicals = technicals;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }
}

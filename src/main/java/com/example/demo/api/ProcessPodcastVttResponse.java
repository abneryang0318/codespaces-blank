package com.example.demo.api;

import com.example.demo.model.PodcastWithTaResponse;

public class ProcessPodcastVttResponse {

    // 清洗後，可直接餵給 AI 的純文字
    private String cleanedText;

    // 清洗後文字丟到 analyze-podcast-with-ta 得到的結果
    private PodcastWithTaResponse analysis;

    public ProcessPodcastVttResponse() {
    }

    public String getCleanedText() {
        return cleanedText;
    }

    public void setCleanedText(String cleanedText) {
        this.cleanedText = cleanedText;
    }

    public PodcastWithTaResponse getAnalysis() {
        return analysis;
    }

    public void setAnalysis(PodcastWithTaResponse analysis) {
        this.analysis = analysis;
    }
}

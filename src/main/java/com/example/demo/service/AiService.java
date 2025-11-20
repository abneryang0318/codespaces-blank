package com.example.demo.service;

import com.example.demo.model.PodcastAnalysisResult;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final GeminiClient geminiClient;

    public AiService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    // 原本的簡單 echo（保留給 debug 用）
    public String echo(String text) {
        if (text == null || text.isBlank()) {
            return "（沒有收到內容）";
        }
        return "你傳給我的內容是：\n" + text;
    }

    // 舊的簡單摘要（/api/analyze-text 用的）
    public String summarizeWithGemini(String text) {
        return geminiClient.summarize(text);
    }

    // 新的：專門給 Podcast 文本分析
    public PodcastAnalysisResult analyzePodcastText(String text) {
        return geminiClient.analyzePodcast(text);
    }
}

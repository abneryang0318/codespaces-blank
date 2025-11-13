package com.example.demo.service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private GenerativeModel geminiProModel; // Gemini AI 模型

    /**
     * 建構子：在 AiService 啟動時，初始化 Gemini 模型
     */
    public AiService() {
        // 1. 從環境變數讀取金鑰
        String apiKey = System.getenv("GOOGLE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.error("!!! GOOGLE_API_KEY 環境變數尚未設定！請在 GitHub Codespaces Secrets 中設定。");
            // 這裡你可以選擇拋出例外，讓 Spring Boot 啟動失敗
            // throw new IllegalStateException("GOOGLE_API_KEY 尚未設定");

            // 或者，你可以先不初始化，讓後面的呼叫失敗 (我們暫時這樣做)
            return; 
        }

        // 2. 初始化 VertexAI (Google AI 的 Java 客戶端)
        // 我們需要指定一個專案 ID (這裡隨便填，因為我們用的是 AI Studio 的金鑰)
        // 和一個地區 (gemini-pro-vision 目前只在特定地區)
        String projectId = "my-side-project";
        String location = "us-central1";

        try (VertexAI vertexAI = new VertexAI(projectId, location, apiKey)) {

            // 3. 選擇我們要用的模型 (gemini-1.0-pro 是個好選擇)
            this.geminiProModel = new GenerativeModel("gemini-1.0-pro", vertexAI);
            log.info("Google AI (Gemini) 服務初始化成功！");

        } catch (Exception e) {
            log.error("Google AI (Gemini) 服務初始化失敗", e);
        }
    }

    /**
     * 【Tool 2】 
     * 萃取 Podcast 逐字稿的重點
     * @param podcastText 來自 Tool 1.5 的純文字字串
     * @return AI 生成的萃取結果 (字串)
     */
    public String extractInfoFromPodcast(String podcastText) {
        if (this.geminiProModel == null) {
            log.error("Gemini 模型尚未初始化，請檢查 API Key 設定。");
            return null;
        }

        // 這是我們的「提示 (Prompt)」
        String prompt = "你是一個專業的財經分析師。請你閱讀以下的 Podcast 逐字稿，" +
                      "並萃取出 3-5 個最重要的關鍵重點，使用條列式清單回覆。\n\n" +
                      "逐字稿：\n" +
                      podcastText;

        try {
            log.info("正在呼叫 Gemini API 進行萃取...");
            // 4. 傳送 Prompt 給 AI 並等待回應
            GenerateContentResponse response = this.geminiProModel.generateContent(prompt);

            // 5. 解析 AI 的回應
            String aiResponseText = ResponseHandler.getText(response);
            log.info("Gemini API 回應成功。");

            return aiResponseText;

        } catch (IOException e) {
            log.error("呼叫 Gemini API 時發生錯誤", e);
            return null;
        }
    }
}
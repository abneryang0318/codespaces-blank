package com.example.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiServiceTest {

    private static final Logger log = LoggerFactory.getLogger(AiServiceTest.class);

    @Autowired
    private AiService aiService; // 自動注入我們的 AI 服務

    @Test
    @DisplayName("【Tool 2 測試】呼叫 Google Gemini API 萃取文字")
    void testExtractInfoFromPodcast() {

        // 1. 準備 (Arrange)
        // 檢查 API Key 是否成功載入並初始化
        // (我們在 AiService 的建構子裡印了日誌，這裡可以再加強)
        assertNotNull(aiService, "AiService 注入失敗");

        // 給 AI 的一段簡單測試文字 (取代 Tool 1.5 的輸出)
        String testText = "大家好，歡迎收聽今天的財經節目。" +
                        "我們來談談台積電。我認為台積電的 3 奈米製程非常強大，" +
                        "而且他們的股價最近表現也很好。";

        // 2. 執行 (Act)
        log.info("--- 正在執行 Tool 2 (AI 萃取) 測試 ---");
        String aiResult = aiService.extractInfoFromPodcast(testText);
        log.info("--- Tool 2 測試執行完畢 ---");


        // 3. 斷言 (Assert)
        assertNotNull(aiResult, "AI 回應不應為 null (請檢查 API Key 和網路連線)");
        assertFalse(aiResult.isBlank(), "AI 回應不應為空白");

        // 檢查 AI 是否真的理解了我們的 Prompt
        String lowerCaseResult = aiResult.toLowerCase();
        assertTrue(lowerCaseResult.contains("台積電") || lowerCaseResult.contains("tsmc"), "AI 回應應包含『台積電』");

        System.out.println("--- 來自 Google AI 的萃取結果 ---");
        System.out.println(aiResult);
        System.out.println("---------------------------------");
    }
}
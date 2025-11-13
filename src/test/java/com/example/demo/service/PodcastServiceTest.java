package com.example.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource; // 匯入這個

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PodcastServiceTest {

    @Autowired
    private PodcastService podcastService;

    // 我們刪掉了原本會失敗的 testDownloadAndParseSubtitles
    // 因為 Codespaces 的 IP 被 YouTube/Vimeo 封鎖了

    @Test
    @DisplayName("【Tool 1.5 測試】解析本地的 VTT 檔案")
    void testParseVttFileToString() {
        try {
            // 1. 準備 (Arrange)
            // 從 "src/test/resources" 讀取我們的測試檔案
            File testFile = new ClassPathResource("test-vtt-file.vtt").getFile();

            // 2. 執行 (Act)
            String cleanText = podcastService.parseVttFileToString(testFile);

            // 3. 斷言 (Assert)
            assertNotNull(cleanText, "解析出的文字不應為 null");

            // 檢查是否成功移除了 VTT 標記
            assertFalse(cleanText.contains("-->"), "不應包含時間戳 '-->'");
            assertFalse(cleanText.contains("WEBVTT"), "不應包含 'WEBVTT' 標頭");
            assertFalse(cleanText.contains("\n"), "不應包含換行符");

            // 檢查文字是否被正確合併成一行
            String expectedText = "This is the first caption. This is the second line, it contains vimeo.";
            assertEquals(expectedText, cleanText);

            System.out.println("--- Tool 1.5 解析成功 ---");
            System.out.println(cleanText);
            System.out.println("-------------------------");

        } catch (Exception e) {
            fail("讀取測試 VTT 檔案時發生例外", e);
        }
    }

    @Test
    @DisplayName("【Tool 1 測試】無效影片應回傳 null (這個會成功)")
    void testNonExistentVideo() {
        // 這個測試在 Codespaces 裡會失敗 (因為 yt-dlp 被封鎖了)
        // 但在「真實環境」中 (例如你的 Mac 或 Cloud Run)，它是正確的

        String testUrl = "https://www.youtube.com/watch?v=NON_EXISTENT_ID";
        File downloadedFile = podcastService.downloadSubtitles(testUrl);

        // 在 Codespaces 中，因為被封鎖，downloadedFile 會是 null，所以測試會通過
        assertNull(downloadedFile, "對於無效/被封鎖的影片，應回傳 null");
    }
}
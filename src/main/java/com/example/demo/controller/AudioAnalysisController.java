package com.example.demo.controller;

import com.example.demo.service.PodcastEnrichmentService;
import com.example.demo.service.WhisperClient; // 引入新的 WhisperClient
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AudioAnalysisController {

    // *** 依賴注入修改 ***
    // 移除 GeminiSttService，加入 WhisperClient
    private final WhisperClient whisperClient;
    private final PodcastEnrichmentService podcastEnrichmentService;

    public AudioAnalysisController(WhisperClient whisperClient,
                                   PodcastEnrichmentService podcastEnrichmentService) {
        this.whisperClient = whisperClient;
        this.podcastEnrichmentService = podcastEnrichmentService;
    }

    @PostMapping(
            path = "/analyze-audio-with-ta",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> analyzeAudioWithTa(
            @RequestPart("file") MultipartFile file
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (file == null || file.isEmpty()) {
            result.put("transcript", null);
            result.put("analysisResult", null);
            result.put("message", "上傳的檔案為空，請確認有選擇音訊檔案。");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        try {
            byte[] audioBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            // *** 核心流程修改 ***
            // 步驟 1: 呼叫 WhisperClient 進行語音轉文字
            String transcript = whisperClient.transcribe(audioBytes, originalFilename);

            // 步驟 2: 將純文字逐字稿交給 EnrichmentService 進行後續分析
            Object analysisResult = podcastEnrichmentService.analyzeWithTa(transcript);

            // 步驟 3: 組合回應 (與之前相同)
            result.put("transcript", transcript);
            result.put("analysisResult", analysisResult);
            result.put("originalFilename", originalFilename);
            result.put("sizeBytes", audioBytes.length);
            result.put("message", "OK");

            return ResponseEntity.ok(result);

        } catch (IOException ex) {
            result.put("transcript", null);
            result.put("analysisResult", null);
            result.put("message", "讀取上傳檔案失敗: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } catch (Exception ex) {
            // 捕捉 Whisper 或 Gemini 可能拋出的例外
            result.put("transcript", null);
            result.put("analysisResult", null);
            result.put("message", "音訊分析過程中發生錯誤: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}
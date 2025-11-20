package com.example.demo.controller;

import com.example.demo.service.GeminiSttService;
import com.example.demo.service.PodcastEnrichmentService;
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

    private final GeminiSttService geminiSttService;
    private final PodcastEnrichmentService podcastEnrichmentService;

    public AudioAnalysisController(GeminiSttService geminiSttService,
                                   PodcastEnrichmentService podcastEnrichmentService) {
        this.geminiSttService = geminiSttService;
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
            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "audio/mpeg";
            }

            String transcript = geminiSttService.transcribe(audioBytes, mimeType);
            Object analysisResult = podcastEnrichmentService.analyzeWithTa(transcript);

            result.put("transcript", transcript);
            result.put("analysisResult", analysisResult);
            result.put("mimeType", mimeType);
            result.put("sizeBytes", audioBytes.length);
            result.put("message", "OK");

            return ResponseEntity.ok(result);
        } catch (IOException ex) {
            result.put("transcript", null);
            result.put("analysisResult", null);
            result.put("message", "讀取上傳檔案失敗: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}

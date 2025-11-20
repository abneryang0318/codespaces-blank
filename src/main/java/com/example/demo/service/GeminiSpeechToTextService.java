package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Base64;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiSpeechToTextService implements SpeechToTextService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String sttModelName;
    private final String defaultTextModelName;

    public GeminiSpeechToTextService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${GOOGLE_API_KEY:}") String apiKey,
            @Value("${GEMINI_STT_MODEL:}") String sttModelName,
            @Value("${GEMINI_MODEL:gemini-2.0-flash-exp}") String defaultTextModelName
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofMinutes(5))
                .build();
        this.apiKey = apiKey;
        this.sttModelName = sttModelName;
        this.defaultTextModelName = defaultTextModelName;
    }

    @Override
    public String transcribe(byte[] audioBytes, String mimeType) {
        if (audioBytes == null || audioBytes.length == 0) {
            return "";
        }
        if (apiKey == null || apiKey.isEmpty()) {
            return "尚未設定 GOOGLE_API_KEY，無法進行語音轉文字。";
        }

        String effectiveMimeType = (mimeType == null || mimeType.isEmpty())
                ? "audio/mpeg"
                : mimeType;

        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

        Map<String, Object> inlineData = new LinkedHashMap<>();
        inlineData.put("mimeType", effectiveMimeType);
        inlineData.put("data", base64Audio);

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put(
                "text",
                "請將以下音訊內容完整轉寫成繁體中文逐字稿，" +
                "保留說話者的口語與語氣，不要總結，不要翻譯成其他語言，" +
                "只輸出逐字稿本身，不要加說明文字。"
        );

        Map<String, Object> audioPart = new LinkedHashMap<>();
        audioPart.put("inlineData", inlineData);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("parts", Arrays.asList(textPart, audioPart));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", 8192);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", Collections.singletonList(content));
        requestBody.put("generationConfig", generationConfig);

        // 如果沒有特別指定 GEMINI_STT_MODEL，就沿用文字分析那顆 GEMINI_MODEL
        String modelToUse;
        if (sttModelName != null && !sttModelName.isEmpty()) {
            modelToUse = sttModelName;
        } else {
            modelToUse = defaultTextModelName;
        }

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/"
                        + modelToUse
                        + ":generateContent?key="
                        + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "呼叫 Gemini API 失敗，狀態碼: " + response.getStatusCodeValue();
            }

            Map<String, Object> body = response.getBody();
            Object candidatesObj = body.get("candidates");
            if (!(candidatesObj instanceof List)) {
                return "Gemini 回傳內容中沒有 candidates 欄位。";
            }

            List<?> candidates = (List<?>) candidatesObj;
            if (candidates.isEmpty()) {
                return "Gemini 沒有回傳任何候選結果。";
            }

            Object firstCandidateObj = candidates.get(0);
            if (!(firstCandidateObj instanceof Map)) {
                return "Gemini 回傳的第一個候選結果格式不符合預期。";
            }

            Map<?, ?> firstCandidate = (Map<?, ?>) firstCandidateObj;
            Object contentObj = firstCandidate.get("content");
            if (!(contentObj instanceof Map)) {
                return "Gemini 回傳內容中缺少 content 欄位。";
            }

            Map<?, ?> contentMap = (Map<?, ?>) contentObj;
            Object partsObj = contentMap.get("parts");
            if (!(partsObj instanceof List)) {
                return "Gemini 回傳內容中缺少 parts 欄位。";
            }

            List<?> parts = (List<?>) partsObj;
            StringBuilder sb = new StringBuilder();
            for (Object partObj : parts) {
                if (partObj instanceof Map) {
                    Object textObj = ((Map<?, ?>) partObj).get("text");
                    if (textObj != null) {
                        sb.append(textObj.toString());
                    }
                }
            }

            if (sb.length() == 0) {
                return "Gemini 回傳內容中沒有文字結果。";
            }

            return sb.toString();

        } catch (RestClientException ex) {
            return "呼叫 Gemini API 發生例外: " + ex.getMessage();
        }
    }
}

package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GeminiSttService {

    // inline_data 給 Gemini 的大小上限（約 20MB）
    private static final int MAX_INLINE_BYTES = 30 * 1024 * 1024;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String modelName;

    /**
     * 這邊直接吃環境變數 GOOGLE_API_KEY，
     * 你已經有用它在 curl 測 ListModels，所以這樣寫是 OK 的。
     *
     * gemini.stt.model 沒設定時預設用 gemini-2.5-flash。
     */
    public GeminiSttService(
            RestTemplate restTemplate,
            @Value("${GOOGLE_API_KEY:}") String apiKey,
            @Value("${gemini.stt.model:gemini-2.5-flash}") String modelName
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    /**
     * 呼叫 Gemini API 做語音轉文字。
     *
     * @param audioBytes 音訊原始位元組
     * @param mimeType   例如 audio/mpeg
     * @return 逐字稿文字，或錯誤訊息
     */
    public String transcribe(byte[] audioBytes, String mimeType) {
        if (audioBytes == null || audioBytes.length == 0) {
            return "音檔為空，無法轉譯。";
        }

        // 檔案太大就不要丟給 Gemini，直接回友善訊息
        if (audioBytes.length > MAX_INLINE_BYTES) {
            double mb = audioBytes.length / 1024.0 / 1024.0;
            return String.format(
                    "音檔太大（約 %.1f MB），目前 STT 單次上限約 20MB，" +
                    "請先壓縮或截短音檔後再上傳。", mb);
        }

        try {
            // v1 endpoint + models/<name>
            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s",
                    modelName, apiKey);

            String base64 = Base64.getEncoder().encodeToString(audioBytes);

            // audio part
            Map<String, Object> inlineData = new LinkedHashMap<>();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64);

            Map<String, Object> audioPart = new LinkedHashMap<>();
            audioPart.put("inline_data", inlineData);

            // prompt part
Map<String, Object> promptPart = new LinkedHashMap<>();
promptPart.put("text",
        "你現在收到的是一段 Podcast 的音訊檔，音檔內容已經透過 API 以 inline_data 的方式提供給你，" +
        "所以不要再要求使用者上傳檔案或貼連結。" +
        "請專心聽這段音訊，幫我做『逐字稿』，" +
        "只要輸出逐字稿本身（繁體中文），不要有任何多餘的說明文字、解釋或條列。");

            // content = [ promptPart, audioPart ]
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("parts", Arrays.asList(promptPart, audioPart));

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("contents", Collections.singletonList(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            String body = response.getBody();
            if (body == null || body.isEmpty()) {
                return "Gemini 回傳空內容，無法解析。";
            }

            // 解析 JSON，把 text 拼起來當成逐字稿
            JsonNode root = objectMapper.readTree(body);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.size() == 0) {
                return "Gemini 回傳內容中沒有 candidate，可疑回應：" + body;
            }

            StringBuilder sb = new StringBuilder();
            JsonNode partsNode = candidates.get(0).path("content").path("parts");
            if (partsNode.isArray()) {
                for (JsonNode p : partsNode) {
                    JsonNode textNode = p.get("text");
                    if (textNode != null && !textNode.isNull()) {
                        sb.append(textNode.asText());
                    }
                }
            }

            String transcript = sb.toString().trim();
            if (transcript.isEmpty()) {
                return "Gemini 回傳內容無法解析成文字，原始回應：" + body;
            }
            return transcript;
        } catch (Exception e) {
            return "呼叫 Gemini API 發生例外: " + e.getMessage();
        }
    }
}

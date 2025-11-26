package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig; // 引入 RequestConfig
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout; // 引入 Timeout
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WhisperClient {

    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final String apiKey;
    private final ObjectMapper objectMapper;

    public WhisperClient(ObjectMapper objectMapper) {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.objectMapper = objectMapper;
    }

    public String transcribe(byte[] audioBytes, String originalFilename) throws IOException, IllegalStateException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("尚未設定 OPENAI_API_KEY 環境變數。");
        }
        if (audioBytes == null || audioBytes.length == 0) {
            return "";
        }

        // *** 核心修改點：建立客製化的 HttpClient，並設定超時時間 ***
// 設定請求逾時時間為 10 分鐘 (600,000 毫秒)
RequestConfig requestConfig = RequestConfig.custom()
        .setResponseTimeout(Timeout.ofMinutes(10))
        .setConnectionRequestTimeout(Timeout.ofMinutes(10))
        .setConnectTimeout(Timeout.ofMinutes(10))
        .build();
        // 將超時設定套用到 HttpClient
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            
            HttpPost httpPost = new HttpPost(WHISPER_API_URL);
            httpPost.setHeader("Authorization", "Bearer " + apiKey);

            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.EXTENDED)
                    .addBinaryBody("file", audioBytes, ContentType.DEFAULT_BINARY, originalFilename)
                    .addTextBody("model", "whisper-1")
                    .addTextBody("language", "zh")
                    .build();

            httpPost.setEntity(multipartEntity);

            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                HttpEntity responseEntity = response.getEntity();
                String responseBody = EntityUtils.toString(responseEntity);

                if (statusCode / 100 != 2) {
                    throw new IllegalStateException(
                            "Whisper API HTTP 狀態碼異常: " + statusCode
                                    + "，回應內容=" + responseBody);
                }

                JsonNode root = objectMapper.readTree(responseBody);
                if (root.has("text")) {
                    return root.get("text").asText();
                } else {
                    throw new IllegalStateException("Whisper API 回應中找不到 'text' 欄位。");
                }
            });
        }
    }
}
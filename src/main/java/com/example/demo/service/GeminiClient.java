package com.example.demo.service;

import com.example.demo.model.PodcastAnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class GeminiClient {

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(ObjectMapper objectMapper) {
        this.apiKey = System.getenv("GOOGLE_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    // ----------------- 1) 簡單文字摘要 -----------------

    public String summarize(String input) {
        if (input == null || input.isBlank()) {
            return "（沒有內容可以總結）";
        }

        if (this.apiKey == null || this.apiKey.isBlank()) {
            return "尚未設定 GOOGLE_API_KEY，無法呼叫 Gemini。";
        }

        try {
            String prompt = "請用繁體中文幫我整理下面這段文字的重點，"
                    + "抓出最重要的 3~5 個重點條列，控制在 300 字以內：\n\n"
                    + input;

            String modelText = callGeminiRawWithRetry(prompt);

            if (modelText == null || modelText.isBlank()) {
                return "Gemini API 沒有回傳任何文字內容。";
            }

            return modelText;
        } catch (Exception e) {
            return "呼叫 Gemini API 失敗：" + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    // ----------------- 2) Podcast JSON 分析 -----------------

   public PodcastAnalysisResult analyzePodcast(String input) {
        if (input == null || input.isBlank()) {
            PodcastAnalysisResult empty = new PodcastAnalysisResult();
            empty.setSummary("（沒有內容可供分析）");
            return empty;
        }

        if (this.apiKey == null || this.apiKey.isBlank()) {
            PodcastAnalysisResult error = new PodcastAnalysisResult();
            error.setSummary("尚未設定 GOOGLE_API_KEY，無法呼叫 Gemini。");
            return error;
        }

        try {
            String prompt =
                    "你是一個財經 Podcast 助理，使用繁體中文回答。\n"
                            + "請針對我提供的節錄文字，分析「講者在節目中對市場與個股的看法」。\n\n"
                            + "你必須只回傳 **JSON**，不要加任何其他說明文字、不要加 markdown、不要加註解。\n\n"
                            + "JSON 結構固定為：\n"
                            + "{\n"
                            + "  \"summary\": \"用繁體中文，簡短總結整集或這段內容的重點\",\n"
                            + "  \"tickers\": [\n"
                            + "    {\n"
                            + "      \"symbol\": \"股票代碼，例如 TSLA、NVDA；若只提公司中文名稱而無法判斷代碼，可以用 null\",\n"
                            + "      \"name\": \"股票或公司名稱（可為 null）\",\n"
                            + "      \"sentiment\": \"bullish 或 bearish 或 neutral\",\n"
                            + "      \"reason\": \"講者對該股票持此看法的原因，簡短一句話\"\n"
                            + "    }\n"
                            + "  ],\n"
                            + "  \"sectors\": [\n"
                            + "    {\n"
                            + "      \"name\": \"產業或主題名稱，例如 AI 半導體、雲端、電動車\",\n"
                            + "      \"sentiment\": \"bullish 或 bearish 或 neutral\",\n"
                            + "      \"reason\": \"講者對該產業的看法與理由，簡短一句話\"\n"
                            + "    }\n"
                            + "  ],\n"
                            + "  \"macroView\": [\n"
                            + "    \"列出講者對總體經濟、利率、通膨、政策、資金行情等觀點，每個元素一個重點句子\"\n"
                            + "  ]\n"
                            + "}\n\n"
                            + "請務必遵守：\n"
                            + "1. 一定要回傳合法的 JSON。\n"
                            + "2. 所有字串內容使用繁體中文。\n"
                            + "3. 若沒有找到任何股票或產業，就使用空陣列 [].\n"
                            + "4. macroView 也可以是空陣列。\n\n"
                            + "以下是節錄的 Podcast 內容：\n\n"
                            + input;

            String modelText = callGeminiRawWithRetry(prompt);

            if (modelText == null || modelText.isBlank()) {
                PodcastAnalysisResult error = new PodcastAnalysisResult();
                error.setSummary("Gemini API 沒有回傳任何文字內容。");
                return error;
            }

            // *** 核心修改点：加入防御性解析逻辑 ***
            String cleanedJson = modelText.trim();
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substring(7); // 移除 ```json
            } else if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substring(3); // 移除 ```
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            cleanedJson = cleanedJson.trim(); // 再次 trim 确保没有多余的换行符

            try {
                // 使用清洗过的 JSON 字符串进行解析
                return objectMapper.readValue(cleanedJson, PodcastAnalysisResult.class);
            } catch (Exception parseError) {
                PodcastAnalysisResult error = new PodcastAnalysisResult();
                error.setSummary("無法將 Gemini 回應解析為 JSON，原始內容如下：\n" + modelText);
                return error;
            }

        } catch (Exception e) {
            PodcastAnalysisResult error = new PodcastAnalysisResult();
            error.setSummary("呼叫 Gemini API 失敗：" + e.getClass().getSimpleName() + " - " + e.getMessage());
            return error;
        }
    }

    // ----------------- 3) 內部：帶 Retry 的呼叫邏輯 -----------------

    private String callGeminiRawWithRetry(String prompt) throws Exception {
        int maxRetries = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            attempt++;
            try {
                return callGeminiOnce(prompt);
            } catch (IllegalStateException e) {
                String msg = e.getMessage();
                // 如果是 503 / UNAVAILABLE，再重試；其他錯誤就直接丟出去
                if (msg != null && msg.contains("status=503")) {
                    lastException = e;
                    // 簡單 backoff
                    Thread.sleep(500L * attempt);
                    continue;
                } else {
                    throw e;
                }
            }
        }

        // 多次重試還是 503，就把最後一次錯誤丟出去
        if (lastException != null) {
            throw lastException;
        } else {
            throw new IllegalStateException("Gemini API 多次呼叫失敗（未知原因）。");
        }
    }

    private String callGeminiOnce(String prompt) throws Exception {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();

        part.put("text", prompt);
        content.put("parts", Collections.singletonList(part));
        body.put("contents", Collections.singletonList(content));

        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_ENDPOINT + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status / 100 != 2) {
            throw new IllegalStateException(
                    "Gemini API 回傳非 2xx 狀態碼，status="
                            + status
                            + "，body=\n"
                            + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");

        if (!candidates.isArray() || candidates.size() == 0) {
            throw new IllegalStateException("Gemini API 沒有回傳任何 candidates，body=\n" + response.body());
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.size() == 0) {
            throw new IllegalStateException("Gemini API 回傳內容裡沒有 parts，body=\n" + response.body());
        }

        StringBuilder sb = new StringBuilder();
        for (JsonNode p : parts) {
            JsonNode textNode = p.get("text");
            if (textNode != null && !textNode.isNull()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(textNode.asText());
            }
        }

        return sb.toString();
    }
}

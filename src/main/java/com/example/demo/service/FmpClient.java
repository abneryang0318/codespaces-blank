package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class FmpClient {

    private static final String FMP_EOD_URL =
            "https://financialmodelingprep.com/stable/historical-price-eod/full";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FmpClient(ObjectMapper objectMapper) {
        // 正式環境用環境變數讀 Key
        this.apiKey = System.getenv("FMP_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    /**
     * 從 FMP 取得美股歷史日 K 資料，並回傳收盤價列表（由舊到新）。
     */
    public List<Double> fetchDailyCloses(String symbol, int days) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("尚未設定 FMP_API_KEY 環境變數。");
        }

        int lookbackDays = days + 30; // 多抓一些天數防假日

        String url = FMP_EOD_URL
                + "?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8)
                + "&timeseries=" + lookbackDays
                + "&apikey=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            if (response.statusCode() == 404) {
                return Collections.emptyList();
            }
            throw new IllegalStateException(
                    "FMP API HTTP 狀態碼異常: " + response.statusCode()
                            + "，body=" + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());

        if (root.has("Error Message")) {
            throw new IllegalStateException("FMP API 回傳錯誤: " + root.get("Error Message").asText());
        }

        // stable/historical-price-eod/full 通常直接回傳陣列，
        // 但舊文件是包在 "historical" 裡，兩種都支援一下
        JsonNode dataNode;
        if (root.isArray()) {
            dataNode = root;
        } else {
            dataNode = root.path("historical");
        }

        if (!dataNode.isArray() || dataNode.isEmpty()) {
            return Collections.emptyList();
        }

        List<Double> closes = new ArrayList<>();
        for (JsonNode dayNode : dataNode) {
            JsonNode closeNode = dayNode.path("close");
            if (closeNode.isNumber()) {
                closes.add(closeNode.asDouble());
            }
        }

        // API 多半是「新到舊」，反轉成「舊到新」
        Collections.reverse(closes);

        if (closes.size() <= days) {
            return closes;
        } else {
            int fromIndex = closes.size() - days;
            return new ArrayList<>(closes.subList(fromIndex, closes.size()));
        }
    }
}

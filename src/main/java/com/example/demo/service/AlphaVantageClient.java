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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Component
public class AlphaVantageClient {

    private static final String BASE_URL = "https://www.alphavantage.co/query";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AlphaVantageClient(ObjectMapper objectMapper) {
        this.apiKey = System.getenv("ALPHAVANTAGE_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    /**
     * 回傳按日期排序的收盤價列表（由舊到新），最多 maxDays 筆。
     */
    public List<Double> fetchDailyCloses(String symbol, int maxDays) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("尚未設定 ALPHAVANTAGE_API_KEY。");
        }

        String function = "TIME_SERIES_DAILY_ADJUSTED";
        String outputSize = maxDays > 100 ? "full" : "compact";

        String url = BASE_URL
                + "?function=" + function
                + "&symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8)
                + "&outputsize=" + outputSize
                + "&apikey=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException(
                    "Alpha Vantage HTTP 狀態碼異常: " + response.statusCode()
                            + "，body=" + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());

        if (root.has("Error Message")) {
            throw new IllegalStateException("Alpha Vantage 回傳錯誤: " + root.get("Error Message").asText());
        }

        if (root.has("Note")) {
            throw new IllegalStateException("Alpha Vantage 回傳 Note（可能是流量限制）: " + root.get("Note").asText());
        }

        JsonNode series = root.path("Time Series (Daily)");
        if (series.isMissingNode() || !series.fieldNames().hasNext()) {
            throw new IllegalStateException("找不到 Time Series (Daily) 資料，回傳內容: " + response.body());
        }

        TreeMap<LocalDate, Double> map = new TreeMap<>();
        series.fieldNames().forEachRemaining(dateStr -> {
            JsonNode dayNode = series.get(dateStr);
            JsonNode closeNode = dayNode.path("4. close");
            if (!closeNode.isMissingNode()) {
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    double close = closeNode.asDouble();
                    map.put(date, close);
                } catch (Exception ignore) {
                    // 略過非法日期或數值
                }
            }
        });

        List<Double> allCloses = new ArrayList<>(map.values());
        if (allCloses.isEmpty()) {
            throw new IllegalStateException("Time Series 解析後沒有任何收盤價資料。");
        }

        int fromIndex = Math.max(0, allCloses.size() - maxDays);
        return new ArrayList<>(allCloses.subList(fromIndex, allCloses.size()));
    }
}

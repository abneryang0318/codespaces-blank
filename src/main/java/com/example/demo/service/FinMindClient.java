package com.example.demo.service;

import com.example.demo.model.FinMindPriceRecord;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class FinMindClient {

    private static final String FINMIND_URL = "https://api.finmindtrade.com/api/v4/data";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;

    public FinMindClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        // 從環境變數讀取 FinMind token
        this.token = System.getenv("FINMIND_API_TOKEN");
    }

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    /**
     * 從 FinMind 取得台股日 K 資料 (TaiwanStockPrice)
     */
    public List<FinMindPriceRecord> getTaiwanDailyPrice(
            String stockId,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {

        String query = String.format(
                "dataset=%s&data_id=%s&start_date=%s&end_date=%s",
                URLEncoder.encode("TaiwanStockPrice", StandardCharsets.UTF_8),
                URLEncoder.encode(stockId, StandardCharsets.UTF_8),
                URLEncoder.encode(startDate.toString(), StandardCharsets.UTF_8),
                URLEncoder.encode(endDate.toString(), StandardCharsets.UTF_8)
        );

        URI uri = URI.create(FINMIND_URL + "?" + query);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .GET();

        if (hasToken()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException(
                    "FinMind API 回傳非 2xx 狀態碼，status="
                            + response.statusCode()
                            + "，body=" + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode dataNode = root.path("data");

        if (!dataNode.isArray()) {
            return Collections.emptyList();
        }

        List<FinMindPriceRecord> records =
                objectMapper.readerForListOf(FinMindPriceRecord.class).readValue(dataNode);

        // 確保依日期排序（由舊到新）
        records.sort(Comparator.comparing(FinMindPriceRecord::getDate));

        return records;
    }
}

package com.example.demo.service;

import com.example.demo.model.StockTaSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TechnicalAnalysisService {

    private final AlphaVantageClient alphaVantageClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TechnicalAnalysisService(AlphaVantageClient alphaVantageClient,
                                    ObjectMapper objectMapper) {
        this.alphaVantageClient = alphaVantageClient;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * 台股技術分析：使用 FinMind 取得收盤價，計算 lastClose、SMA20、RSI14。
     */
    public StockTaSummary analyzeTaiwanStock(String symbol, int days) {
        StockTaSummary summary = new StockTaSummary();
        summary.setSymbol(symbol);
        summary.setMarket("TW");

        String token = System.getenv("FINMIND_API_TOKEN");
        if (token == null || token.isBlank()) {
            summary.setMessage("尚未設定 FINMIND_API_TOKEN，請先在環境變數或 Codespaces Secret 中設定。");
            return summary;
        }

        try {
            LocalDate end = LocalDate.now();
            // 多抓一點天數，避免中間有停牌或假日導致資料不足
            LocalDate start = end.minusDays(days + 30L);

            String url = "https://api.finmindtrade.com/api/v4/data"
                    + "?dataset=TaiwanStockPrice"
                    + "&data_id=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8)
                    + "&start_date=" + start
                    + "&end_date=" + end
                    + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                summary.setMessage("FinMind HTTP 狀態碼異常: " + response.statusCode());
                return summary;
            }

            JsonNode root = objectMapper.readTree(response.body());

            String msg = root.path("msg").asText("");
            if (!"success".equalsIgnoreCase(msg)) {
                summary.setMessage("FinMind 回傳錯誤: " + msg);
                return summary;
            }

            JsonNode dataNode = root.path("data");
            if (!dataNode.isArray() || dataNode.isEmpty()) {
                summary.setMessage("FinMind 沒有回傳任何收盤價資料。");
                return summary;
            }

            List<LocalDate> dates = new ArrayList<>();
            List<Double> closes = new ArrayList<>();

            for (JsonNode node : dataNode) {
                String dateStr = node.path("date").asText(null);
                JsonNode closeNode = node.path("close");
                if (dateStr == null || closeNode.isMissingNode()) {
                    continue;
                }
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    double close = closeNode.asDouble();
                    dates.add(date);
                    closes.add(close);
                } catch (Exception ignore) {
                    // 略過格式不正確的資料
                }
            }

            if (closes.isEmpty()) {
                summary.setMessage("FinMind 沒有回傳任何有效收盤價資料。");
                return summary;
            }

            int size = closes.size();
            int fromIndex = Math.max(0, size - days);
            List<Double> closesWindow = closes.subList(fromIndex, size);
            List<LocalDate> datesWindow = dates.subList(fromIndex, size);

            int windowSize = closesWindow.size();
            double lastClose = closesWindow.get(windowSize - 1);
            int smaPeriod = Math.min(20, windowSize);
            double sma20 = calculateSma(closesWindow, smaPeriod);
            double rsi14 = windowSize >= 15 ? calculateRsi(closesWindow, 14) : 0.0;

            // 注意：StockTaSummary 的 startDate / endDate 是 String 型別
            summary.setStartDate(datesWindow.get(0).toString());
            summary.setEndDate(datesWindow.get(windowSize - 1).toString());
            summary.setLastClose(lastClose);
            summary.setSma20(sma20);
            summary.setRsi14(rsi14);
            summary.setAboveSma20(lastClose >= sma20);
            summary.setMessage("OK");

            return summary;
        } catch (Exception e) {
            String error = e.getMessage();
            if (error == null || error.isBlank()) {
                error = "FinMind 分析失敗。";
            }
            summary.setMessage(error);
            return summary;
        }
    }

    /**
     * 美股技術分析：使用 Alpha Vantage 取得收盤價，計算 lastClose、SMA20、RSI14。
     */
    public StockTaSummary analyzeUsStock(String symbol, int days) {
        StockTaSummary summary = new StockTaSummary();
        summary.setSymbol(symbol);
        summary.setMarket("US");

        try {
            List<Double> closes = alphaVantageClient.fetchDailyCloses(symbol, days);
            if (closes == null || closes.isEmpty()) {
                summary.setMessage("Alpha Vantage 沒有回傳任何收盤價資料。");
                return summary;
            }

            int size = closes.size();
            double lastClose = closes.get(size - 1);

            int smaPeriod = Math.min(20, size);
            double sma20 = calculateSma(closes, smaPeriod);

            double rsi14 = 0.0;
            if (size >= 15) {
                rsi14 = calculateRsi(closes, 14);
            }

            // 這裡先用「今天往回推 size 天」當作日期區間
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(size - 1L);

            summary.setStartDate(start.toString());
            summary.setEndDate(end.toString());
            summary.setLastClose(lastClose);
            summary.setSma20(sma20);
            summary.setRsi14(rsi14);
            summary.setAboveSma20(lastClose >= sma20);
            summary.setMessage("OK");

            return summary;
        } catch (Exception e) {
            String error = e.getMessage();
            if (error == null || error.isBlank()) {
                error = "Alpha Vantage 分析失敗。";
            }
            summary.setMessage(error);
            return summary;
        }
    }

    /**
     * 計算簡單移動平均。
     */
    private double calculateSma(List<Double> closes, int period) {
        if (closes == null || closes.isEmpty() || period <= 0) {
            return 0.0;
        }
        int size = closes.size();
        int from = Math.max(0, size - period);
        double sum = 0.0;
        for (int i = from; i < size; i++) {
            sum += closes.get(i);
        }
        int actualPeriod = size - from;
        return actualPeriod == 0 ? 0.0 : sum / actualPeriod;
    }

    /**
     * 計算 RSI（簡單版）。
     */
    private double calculateRsi(List<Double> closes, int period) {
        if (closes == null || closes.size() <= period || period <= 0) {
            return 0.0;
        }

        int size = closes.size();
        double gain = 0.0;
        double loss = 0.0;

        int start = size - period - 1;
        int end = size - 1;

        for (int i = start; i < end; i++) {
            double diff = closes.get(i + 1) - closes.get(i);
            if (diff > 0) {
                gain += diff;
            } else if (diff < 0) {
                loss -= diff;
            }
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        if (avgGain == 0.0 && avgLoss == 0.0) {
            return 50.0;
        }
        if (avgLoss == 0.0) {
            return 100.0;
        }
        if (avgGain == 0.0) {
            return 0.0;
        }

        double rs = avgGain / avgLoss;
        return 100.0 - 100.0 / (1.0 + rs);
    }
}

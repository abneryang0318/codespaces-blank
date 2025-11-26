package com.example.demo.service;

import com.example.demo.model.PodcastAnalysisResult;
import com.example.demo.model.PodcastWithTaResponse;
import com.example.demo.model.StockTaSummary;
import com.example.demo.model.TickerView;
import com.example.demo.model.SectorView;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PodcastEnrichmentService {

    private final AiService aiService;
    private final TechnicalAnalysisService technicalAnalysisService;

    public PodcastEnrichmentService(AiService aiService,
                                    TechnicalAnalysisService technicalAnalysisService) {
        this.aiService = aiService;
        this.technicalAnalysisService = technicalAnalysisService;
    }

    public PodcastWithTaResponse analyzeWithTa(String text) {
        PodcastWithTaResponse response = new PodcastWithTaResponse();

        PodcastAnalysisResult analysis = aiService.analyzePodcastText(text);
        response.setAnalysis(analysis);

        String summary = analysis.getSummary();
        if (summary != null && summary.startsWith("呼叫 Gemini API 失敗")) {
            response.setTechnicals(Collections.emptyMap());
            response.setMessage("文字分析失敗（" + summary + "），故未執行技術分析。");
            return response;
        }

        Map<String, StockTaSummary> techMap = new LinkedHashMap<>();
        Set<String> symbolSet = new LinkedHashSet<>();

        List<TickerView> tickers = analysis.getTickers();
        if (tickers != null) {
            for (TickerView t : tickers) {
                if (t == null) {
                    continue;
                }
                String symbol = t.getSymbol();
                if (symbol == null) {
                    continue;
                }
                String trimmed = symbol.trim();
                if (!trimmed.isEmpty()) {
                    symbolSet.add(trimmed);
                }
            }
        }

        if (text != null && !text.isBlank()) {
            Pattern p = Pattern.compile("\\b(\\d{4})\\b");
            Matcher m = p.matcher(text);
            while (m.find()) {
                String code = m.group(1);
                symbolSet.add(code);
            }
        }

int twCount = 0;
int usCount = 0;
int skippedCount = 0;

for (String symbol : symbolSet) {
    // *** 這是我們加入的優化判斷 ***
    if (symbol == null || symbol.equalsIgnoreCase("null") || symbol.isBlank()) {
        continue; // 跳過無效的 symbol，例如 Gemini 回傳的 null 或空字串
    }
    
    String trimmed = symbol.trim().toUpperCase(); // 統一轉為大寫，增加比對穩定性

    // 台股：4 位數字，例如 2330
    if (trimmed.matches("\\d{4}")) {
        StockTaSummary ta = technicalAnalysisService.analyzeTaiwanStock(trimmed, 120);
        techMap.put(trimmed, ta);
        twCount++;
    }
    // 美股：1~5 個英文字母或帶一個點，例如 TSLA、NVDA、BRK.B
    // 這裡我們也檢查一下，確保不是 "NULL" 這個字串
    else if (!trimmed.equals("NULL") && trimmed.matches("^[A-Z\\.]{1,5}$")) {
        StockTaSummary ta = technicalAnalysisService.analyzeUsStock(trimmed, 120);
        techMap.put(trimmed, ta);
        usCount++;
    } else {
        skippedCount++;
    }
}

response.setTechnicals(techMap);

StringBuilder msg = new StringBuilder();
msg.append("已完成文字分析");
if (twCount > 0) {
    msg.append("，並對 ").append(twCount).append(" 檔台股執行技術分析。");
}
if (usCount > 0) {
    msg.append(" 並對 ").append(usCount).append(" 檔美股執行技術分析。");
}
if (twCount == 0 && usCount == 0) {
    msg.append("，目前未偵測到可分析的股票代碼，故未執行技術分析。");
}
if (skippedCount > 0) {
    msg.append(" 共有 ").append(skippedCount).append(" 檔代碼格式不明或暫不支援，略過。");
}

// 這裡加上一行，呼叫我們即將新增的 helper
    response.setFinalAnswer(buildFinalAnswer(analysis, techMap));

response.setMessage(msg.toString());


        return response;
    }

  private String buildFinalAnswer(PodcastAnalysisResult analysis,
                                java.util.Map<String, StockTaSummary> technicals) {
    StringBuilder sb = new StringBuilder();

    // 1) 本集重點 (保持不變)
    if (analysis != null
            && analysis.getSummary() != null
            && !analysis.getSummary().isBlank()) {
        sb.append("本集重點整理：\n");
        sb.append(analysis.getSummary()).append("\n\n");
    }

    // 2) 個股整理
    if (analysis != null
            && analysis.getTickers() != null
            && !analysis.getTickers().isEmpty()) {
        sb.append("個股整理：\n");
        for (TickerView ticker : analysis.getTickers()) {
            String symbol = ticker.getSymbol();
            String name = ticker.getName();
            String sentiment = ticker.getSentiment();
            String reason = ticker.getReason();

            // 如果 symbol 是 null 或無效，就跳過這個 ticker 的處理
            if (symbol == null || symbol.equalsIgnoreCase("null") || symbol.isBlank()) {
                continue;
            }

            String displayName = (name != null && !name.isBlank())
                    ? name + " (" + symbol.toUpperCase() + ")"
                    : symbol.toUpperCase();

            sb.append("- ").append(displayName).append("：");

            String sentimentText = toChineseSentiment(sentiment);
            sb.append("節目整體看法偏向 ").append(sentimentText);

            if (reason != null && !reason.isBlank()) {
                sb.append("，主要理由：").append(reason);
            } else {
                sb.append("。");
            }

            StockTaSummary ta = (technicals != null)
                    ? technicals.get(symbol.toUpperCase()) // 確保用大寫字母查詢
                    : null;

            if (ta != null) {
                sb.append(" 技術面：");
                String taMessage = ta.getMessage();

                if ("OK".equalsIgnoreCase(taMessage)) {
                    double lastClose = ta.getLastClose();
                    double sma20 = ta.getSma20();
                    double rsi14 = ta.getRsi14();

                    if (lastClose > 0) {
                        sb.append("最近收盤價約 ").append(String.format(java.util.Locale.US, "%.2f", lastClose)).append("，");
                    }

                    if (sma20 > 0) {
                        if (ta.isAboveSma20()) {
                            sb.append("位於 20 日均線之上，");
                        } else {
                            sb.append("位於 20 日均線之下，");
                        }
                    }

                    if (rsi14 > 0) {
                        sb.append("RSI 約 ")
                                .append(String.format(java.util.Locale.US, "%.2f", rsi14))
                                .append("。");
                    } else {
                        sb.append("RSI 數值暫缺。");
                    }
                } else {
                    // *** 核心修改點：優雅地處理錯誤訊息 ***
                    if (taMessage != null && taMessage.contains("402") && taMessage.contains("subscription")) {
                        sb.append("免費方案不支援此股票的查詢，暫無技術分析資料。");
                    } else if (taMessage != null && taMessage.contains("API")) {
                        sb.append("因外部 API 問題，暫時無法取得技術分析資料。");
                    } else {
                        sb.append("資料暫缺或查詢失敗。");
                    }
                }
            }

            sb.append("\n");
        }
        sb.append("\n");
    }

    // 3) 產業與主題 (保持不變)
    if (analysis != null
            && analysis.getSectors() != null
            && !analysis.getSectors().isEmpty()) {
        sb.append("產業與主題觀察：\n");
        for (SectorView sector : analysis.getSectors()) {
            String name = sector.getName();
            String sentiment = sector.getSentiment();
            String reason = sector.getReason();

            sb.append("- ")
                    .append(name != null && !name.isBlank() ? name : "未命名產業")
                    .append("：整體看法偏向 ")
                    .append(toChineseSentiment(sentiment));

            if (reason != null && !reason.isBlank()) {
                sb.append("，理由：").append(reason);
            } else {
                sb.append("。");
            }

            sb.append("\n");
        }
        sb.append("\n");
    }

    // 4) 宏觀市場 (保持不變)
    if (analysis != null
            && analysis.getMacroView() != null
            && !analysis.getMacroView().isEmpty()) {
        sb.append("整體市場觀察：\n");
        for (String line : analysis.getMacroView()) {
            sb.append("- ").append(line).append("\n");
        }
    }

    return sb.toString().trim();
}


private String toChineseSentiment(String sentiment) {
    if (sentiment == null || sentiment.isBlank()) {
        return "中性或未明確說明";
    }
    String s = sentiment.toLowerCase(java.util.Locale.ROOT);
    switch (s) {
        case "bullish":
            return "偏多";
        case "bearish":
            return "偏空";
        case "neutral":
        default:
            return "中性或觀望";
    }
}


}

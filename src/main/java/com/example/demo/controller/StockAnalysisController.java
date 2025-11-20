package com.example.demo.controller;

import com.example.demo.model.StockTaSummary;
import com.example.demo.service.TechnicalAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StockAnalysisController {

    private final TechnicalAnalysisService technicalAnalysisService;

    public StockAnalysisController(TechnicalAnalysisService technicalAnalysisService) {
        this.technicalAnalysisService = technicalAnalysisService;
    }

    /**
     * 台股 TA Demo:
     * 例：
     *   GET /api/ta-demo?symbol=2330&days=120
     */
    @GetMapping("/ta-demo")
    public ResponseEntity<StockTaSummary> taDemo(
            @RequestParam(name = "symbol", defaultValue = "2330") String symbol,
            @RequestParam(name = "days", defaultValue = "120") int days
    ) {
        StockTaSummary result = technicalAnalysisService.analyzeTaiwanStock(symbol, days);
        return ResponseEntity.ok(result);
    }

    /**
     * 美股 TA Demo:
     * 例：
     *   GET /api/ta-us-demo?symbol=TSLA&days=120
     */
    @GetMapping("/ta-us-demo")
    public ResponseEntity<StockTaSummary> usTaDemo(
            @RequestParam(name = "symbol") String symbol,
            @RequestParam(name = "days", defaultValue = "120") int days
    ) {
        StockTaSummary result = technicalAnalysisService.analyzeUsStock(symbol, days);
        return ResponseEntity.ok(result);
    }
}

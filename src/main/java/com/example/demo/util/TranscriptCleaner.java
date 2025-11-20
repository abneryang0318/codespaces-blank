package com.example.demo.util;

import java.util.ArrayList;
import java.util.List;

public final class TranscriptCleaner {

    private TranscriptCleaner() {
    }

    public static String clean(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String[] lines = rawText.split("\\R");
        List<String> resultLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();

            // 跳過完全空白行
            if (trimmed.isEmpty()) {
                continue;
            }

            // 跳過 WEBVTT 標頭
            if (trimmed.equalsIgnoreCase("WEBVTT")) {
                continue;
            }

            // 跳過 Kind: / Language: 之類 metadata 行
            String lower = trimmed.toLowerCase();
            if (lower.startsWith("kind:") || lower.startsWith("language:")) {
                continue;
            }

            // 跳過時間戳行，例如：
            // 00:00:01.000 --> 00:00:04.000
            // 00:10:05,123 --> 00:10:10,456
            if (trimmed.matches("^\\d{2}:\\d{2}:\\d{2}[\\.,]\\d{3} --> .*")) {
                continue;
            }

            // 跳過純數字行（例如 SRT 的序號）
            if (trimmed.matches("^\\d+$")) {
                continue;
            }

            resultLines.add(trimmed);
        }

        // 最後再把多行合成一個大段文字
        // 這裡用單一換行符號連接，之後你要改成空白或段落分組都可以
        return String.join("\n", resultLines).trim();
    }
}

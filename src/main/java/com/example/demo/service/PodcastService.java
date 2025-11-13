package com.example.demo.service; // 注意：package 名稱要符合你的資料夾結構

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PodcastService {

    private static final Logger log = LoggerFactory.getLogger(PodcastService.class);

    // 定義一個暫存字幕檔的資料夾
    // VScode Codespaces 會在你的專案根目錄 (demo 資料夾同層) 建立
    private final Path subtitleDirectory = Paths.get("temp-subtitles");

// 【Tool 1.5】 VTT 檔案中繼資料(時間戳、標頭等)的正則表達式
// (這是一個更強大的版本，可以移除 KIND:..., LANGUAGE:... 和純空行)
private static final Pattern VTT_METADATA_PATTERN = Pattern.compile(
        "^(WEBVTT|KIND:.*|LANGUAGE:.*|STYLE::|NOTE|\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s-->\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*|$)(\\R?)|(^\\s*$)(\\R?)",
        Pattern.MULTILINE
);

    /**
     * 【Tool 1】
     * 下載指定 YouTube URL 的字幕檔
     *
     * @param youtubeUrl 影片網址
     * @return 下載的字幕檔 (vtt 檔)；如果失敗則回傳 null
     */
    public File downloadSubtitles(String youtubeUrl) {
        try {
            // 1. 確保暫存資料夾存在
            File directory = subtitleDirectory.toFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 2. 準備 yt-dlp 指令 (使用我們討論過的優化版本)
            String fileId = UUID.randomUUID().toString();
            String outputTemplate = subtitleDirectory.resolve(fileId).toString();

ProcessBuilder processBuilder = new ProcessBuilder(
            "yt-dlp",
            "--write-sub",
            "--write-auto-sub",
            "--sub-lang", "zh-Hant,en,zh-Hans",
            "--skip-download",
            "--sub-format", "vtt",
            "--extractor-args", "youtube:player_client=default", // <--【新增這行】
            "-o", outputTemplate,
            youtubeUrl
    );

            // 3. 啟動 Process
            log.info("執行 yt-dlp 指令 (URL: {})", youtubeUrl);
            Process process = processBuilder.start();

            // 4. 【重要】即時讀取輸出，避免緩衝區卡死
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), log::info);
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), log::error);
            new Thread(outputGobbler).start();
            new Thread(errorGobbler).start();

            // 5. 等待指令執行完畢
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // 6. 找出下載的檔案
                File downloadedFile = findDownloadedFile(fileId);

                if (downloadedFile != null) {
                    log.info("字幕下載成功: {}", downloadedFile.getName());
                    return downloadedFile;
                } else {
                    log.warn("yt-dlp 執行成功，但找不到 .vtt 檔案 (FileID: {})", fileId);
                    return null;
                }
            } else {
                log.error("yt-dlp 執行失敗，exit code: {}", exitCode);
                return null;
            }

        } catch (Exception e) {
            log.error("下載字幕時發生例外", e);
            return null;
        }
    }

    /**
     * 【Tool 1.5】
     * 解析 VTT 檔案，轉換為乾淨的純文字字串
     *
     * @param vttFile 從 Tool 1 拿到的 .vtt 檔案
     * @return 適合餵給 AI 的單一純文字字串
     */
    public String parseVttFileToString(File vttFile) {
        if (vttFile == null || !vttFile.exists()) {
            return null;
        }

        try {
            String rawContent = Files.readString(vttFile.toPath());

            // 1. 移除所有 VTT 標記和時間戳
            String textOnly = VTT_METADATA_PATTERN.matcher(rawContent).replaceAll("");

            // 2. 合併所有文字行，並去除多餘的換行
            String cleanText = textOnly.lines()
                    .filter(line -> !line.isBlank())
                    .collect(Collectors.joining(" ")); // 用「空格」把所有行串起來

            log.info("VTT 檔案解析成功，共 {} 字元", cleanText.length());
            return cleanText;

        } catch (Exception e) {
            log.error("解析 VTT 檔案時發生例外: {}", vttFile.getName(), e);
            return null;
        }
    }

    // --- 輔助方法 ---

    /**
     * 輔助方法：找出實際下載的 vtt 檔案
     */
    private File findDownloadedFile(String fileId) {
        File dir = subtitleDirectory.toFile();
        File[] matchingFiles = dir.listFiles((d, name) ->
                name.startsWith(fileId) && name.endsWith(".vtt")
        );

        if (matchingFiles != null && matchingFiles.length > 0) {
            return matchingFiles[0]; // 簡單起見，先回傳第一個
        }
        return null;
    }

    /**
     * 輔助內部類別：用來讀取 Process 輸出的「串流吞噬者」
     */
    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                reader.lines().forEach(consumer);
            } catch (Exception e) {
                // Process 關閉時的例外，可忽略
            }
        }
    }
}
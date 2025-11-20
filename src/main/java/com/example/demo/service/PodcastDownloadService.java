package com.example.demo.service;

import com.example.demo.model.PodcastDownloadResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PodcastDownloadService {

    private final HttpClient httpClient;

    public PodcastDownloadService() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public PodcastDownloadResult download(String audioUrl) {
        PodcastDownloadResult result = new PodcastDownloadResult();
        result.setAudioUrl(audioUrl);

        if (audioUrl == null || audioUrl.isBlank()) {
            result.setMessage("audioUrl 不可為空");
            return result;
        }

        try {
            URI uri = URI.create(audioUrl);

            // 建立暫存檔案
            String fileSuffix = guessSuffixFromUrl(audioUrl);
            Path tempFile = Files.createTempFile("podcast-", fileSuffix);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<Path> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofFile(tempFile)
            );

            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                result.setMessage("下載失敗，HTTP 狀態碼: " + statusCode);
                return result;
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("application/octet-stream");

            long size = Files.size(tempFile);

            result.setFileName(tempFile.getFileName().toString());
            result.setContentType(contentType);
            result.setSizeBytes(size);
            result.setTempFilePath(tempFile.toAbsolutePath().toString());
            result.setMessage("OK");

            return result;

        } catch (IOException e) {
            result.setMessage("下載過程發生 I/O 例外: " + e.getMessage());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.setMessage("下載過程被中斷");
            return result;
        } catch (IllegalArgumentException e) {
            result.setMessage("audioUrl 格式不合法: " + e.getMessage());
            return result;
        }
    }

    private String guessSuffixFromUrl(String audioUrl) {
        String lower = audioUrl.toLowerCase();
        if (lower.contains(".mp3")) {
            return ".mp3";
        }
        if (lower.contains(".m4a")) {
            return ".m4a";
        }
        if (lower.contains(".aac")) {
            return ".aac";
        }
        if (lower.contains(".wav")) {
            return ".wav";
        }
        return ".bin";
    }
}

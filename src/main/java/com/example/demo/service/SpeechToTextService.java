package com.example.demo.service;

public interface SpeechToTextService {

    /**
     * 將音訊內容轉成文字逐字稿。
     *
     * @param audioBytes 音訊檔的位元組內容
     * @param mimeType   音訊檔的 MIME 類型，例如 audio/mpeg
     * @return 轉譯後的逐字稿文字
     */
    String transcribe(byte[] audioBytes, String mimeType);
}

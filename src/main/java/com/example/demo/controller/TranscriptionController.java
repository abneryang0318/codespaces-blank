package com.example.demo.controller;

import com.example.demo.model.TranscriptionResponse;
import com.example.demo.service.SpeechToTextService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class TranscriptionController {

    private final SpeechToTextService speechToTextService;

    public TranscriptionController(SpeechToTextService speechToTextService) {
        this.speechToTextService = speechToTextService;
    }

    /**
     * 將上傳的音訊檔轉成逐字稿。
     *
     * 範例呼叫：
     * curl -X POST http://localhost:8080/api/transcribe-audio \
     *   -F "file=@sample.mp3"
     */
    @PostMapping(
            path = "/transcribe-audio",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<TranscriptionResponse> transcribeAudio(
            @RequestPart("file") MultipartFile file
    ) {
        TranscriptionResponse response = new TranscriptionResponse();

        if (file == null || file.isEmpty()) {
            response.setTranscript(null);
            response.setMimeType(null);
            response.setSizeBytes(0L);
            response.setMessage("上傳的檔案為空，請確認有選擇音訊檔案。");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            byte[] audioBytes = file.getBytes();
            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isEmpty()) {
                // 若無法從上傳內容判斷，就給一個預設值
                mimeType = "audio/mpeg";
            }

            String transcript = speechToTextService.transcribe(audioBytes, mimeType);

            response.setTranscript(transcript);
            response.setMimeType(mimeType);
            response.setSizeBytes(audioBytes.length);
            response.setMessage("OK");

            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            response.setTranscript(null);
            response.setMimeType(null);
            response.setSizeBytes(0L);
            response.setMessage("讀取上傳檔案失敗: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

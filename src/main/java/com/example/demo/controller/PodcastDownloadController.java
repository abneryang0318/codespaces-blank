package com.example.demo.controller;

import com.example.demo.model.PodcastDownloadRequest;
import com.example.demo.model.PodcastDownloadResult;
import com.example.demo.service.PodcastDownloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PodcastDownloadController {

    private final PodcastDownloadService podcastDownloadService;

    public PodcastDownloadController(PodcastDownloadService podcastDownloadService) {
        this.podcastDownloadService = podcastDownloadService;
    }

    @PostMapping("/download-podcast-audio")
    public ResponseEntity<PodcastDownloadResult> download(@RequestBody PodcastDownloadRequest request) {
        PodcastDownloadResult result = podcastDownloadService.download(request.getAudioUrl());
        return ResponseEntity.ok(result);
    }
}
